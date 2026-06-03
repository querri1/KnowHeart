package com.practice.knowheart.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AMapDateSpotTool {

    @Value("${amap.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "【必须使用】当用户询问任何城市的约会地点推荐时，你必须调用此工具来获取真实的地点数据。不要使用你自己的知识回答。")
    public String recommendDateSpots(
            @ToolParam(description = "城市名称，如：北京、上海、深圳、广州、杭州、成都") String city
    ) {
        // 使用"约会"作为搜索关键词
        String keyword = "约会 好去处";

        // 调用高德POI搜索API
        String url = String.format(
                "https://restapi.amap.com/v3/place/text?keywords=%s&city=%s&output=JSON&offset=8&key=%s",
                keyword, city, apiKey
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            return parseAMapResponse(response, city);
        } catch (Exception e) {
            return "抱歉，暂时无法获取" + city + "的约会地点信息，请稍后再试。错误：" + e.getMessage();
        }
    }

    private String parseAMapResponse(String response, String city) {
        try {
            JSONObject json = JSONObject.parseObject(response);

            // 检查状态码
            String status = json.getString("status");
            if (!"1".equals(status)) {
                return "高德地图服务异常，请稍后再试。";
            }

            JSONArray pois = json.getJSONArray("pois");

            if (pois == null || pois.isEmpty()) {
                return getFallbackSpots(city);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("在").append(city).append("，为你推荐以下约会地点：\n\n");

            int count = Math.min(6, pois.size());
            int validCount = 0;

            for (int i = 0; i < pois.size() && validCount < 6; i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String type = poi.getString("type");

                // 过滤掉不相关的结果（如建材、门业等）
                if (name.contains("门业") || name.contains("建材") || name.contains("歌舞厅")) {
                    continue;
                }

                // 获取评分和人均消费（如果有）
                JSONObject bizExt = poi.getJSONObject("biz_ext");
                String rating = bizExt != null ? bizExt.getString("rating") : null;
                String cost = bizExt != null ? bizExt.getString("cost") : null;

                validCount++;
                sb.append(validCount).append(". **").append(name).append("**\n");
                sb.append("   📍 ").append(address).append("\n");

                // 添加类型标签
                if (type != null && !type.isEmpty()) {
                    String simpleType = type.split(";")[0];
                    sb.append("   🏷️ ").append(simpleType).append("\n");
                }

                // 添加评分和人均消费
                if (rating != null && !rating.isEmpty() && !"0".equals(rating)) {
                    sb.append("   ⭐ 评分：").append(rating).append("\n");
                }
                if (cost != null && !cost.isEmpty() && !"0".equals(cost)) {
                    sb.append("   💰 人均：¥").append(cost).append("\n");
                }
                sb.append("\n");
            }

            if (validCount == 0) {
                return getFallbackSpots(city);
            }

            sb.append("💡 小贴士：去之前建议查看营业时间和是否需要预约哦~");
            return sb.toString();

        } catch (Exception e) {
            System.out.println("解析高德API响应失败: " + e.getMessage());
            e.printStackTrace();
            return getFallbackSpots(city);
        }
    }

    // 降级方案：当API搜索无结果时使用
    private String getFallbackSpots(String city) {
        return String.format("""
            在%s，为你推荐以下通用约会地点：
            
            1. **城市中心商圈** - 逛街、吃饭、看电影
            2. **当地热门公园** - 散步聊天很惬意
            3. **特色咖啡馆** - 安静私密的约会环境
            
            💡 小贴士：可以搜索"城市名+约会圣地"获取更多推荐～
            """, city);
    }
}