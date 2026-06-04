package com.practice.knowheart.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WeatherTool {

    @Value("${amap.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "查询指定城市的天气情况，用于帮助用户判断是否适合约会。返回天气、温度、风向等信息")
    public String getWeather(
            @ToolParam(description = "城市名称，如：北京、上海、广州、深圳、成都") String city
    ) {
        System.out.println("========== 天气工具被调用了！城市：" + city + " ==========");

        try {
            // 直接使用城市名查询天气（高德支持中文城市名）
            String url = String.format(
                    "https://restapi.amap.com/v3/weather/weatherInfo?city=%s&key=%s&extensions=all",
                    city, apiKey
            );

            System.out.println("请求URL: " + url);
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("API返回: " + response);

            return parseWeatherResponse(response, city);

        } catch (Exception e) {
            System.out.println("天气查询异常: " + e.getMessage());
            e.printStackTrace();
            return getFallbackWeather(city);
        }
    }

    /**
     * 解析天气响应
     */
    private String parseWeatherResponse(String response, String city) {
        try {
            JSONObject json = JSONObject.parseObject(response);

            String status = json.getString("status");
            String infocode = json.getString("infocode");

            System.out.println("status: " + status + ", infocode: " + infocode);

            if (!"1".equals(status)) {
                return "高德天气服务返回异常，请稍后再试。";
            }

            // 获取 forecasts 数组
            JSONArray forecasts = json.getJSONArray("forecasts");
            if (forecasts == null || forecasts.isEmpty()) {
                return "未找到" + city + "的天气数据，请尝试其他城市。";
            }

            JSONObject forecast = forecasts.getJSONObject(0);
            String province = forecast.getString("province");
            String cityName = forecast.getString("city");
            String reportTime = forecast.getString("reporttime");

            // 获取 casts 数组（天气预报）
            JSONArray casts = forecast.getJSONArray("casts");
            if (casts == null || casts.isEmpty()) {
                return "未找到" + city + "的天气预报数据。";
            }

            // 获取今天（第一天）的天气
            JSONObject today = casts.getJSONObject(0);

            String date = today.getString("date");
            String dayWeather = today.getString("dayweather");
            String nightWeather = today.getString("nightweather");
            String dayTemp = today.getString("daytemp");
            String nightTemp = today.getString("nighttemp");
            String dayWind = today.getString("daywind");
            String nightWind = today.getString("nightwind");
            String dayPower = today.getString("daypower");
            String nightPower = today.getString("nightpower");

            // 生成约会建议
            String advice = getDateAdvice(dayWeather, safeParseInt(dayTemp));

            StringBuilder sb = new StringBuilder();
            sb.append("☀️ **").append(province).append(cityName).append(" 天气**\n\n");
            sb.append("📅 日期：").append(date).append("\n");
            sb.append("🌡️ 白天温度：").append(dayTemp).append("°C\n");
            sb.append("🌙 夜间温度：").append(nightTemp).append("°C\n");
            sb.append("☁️ 白天天气：").append(dayWeather).append("\n");
            sb.append("🌙 夜间天气：").append(nightWeather).append("\n");
            sb.append("💨 风向：白天 ").append(dayWind).append(" ").append(dayPower).append("级\n");
            sb.append("💨 夜间风向：").append(nightWind).append(" ").append(nightPower).append("级\n\n");
            sb.append("💡 **约会建议**：").append(advice);

            return sb.toString();

        } catch (Exception e) {
            System.out.println("解析天气响应失败: " + e.getMessage());
            e.printStackTrace();
            return getFallbackWeather(city);
        }
    }

    private int safeParseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 20; // 默认温度
        }
    }

    /**
     * 根据天气给出约会建议
     */
    private String getDateAdvice(String weather, int temp) {
        if (weather == null) {
            return "建议出门前查看实时天气，祝你们约会愉快！💕";
        }

        if (weather.contains("雨") || weather.contains("雪")) {
            return "今天有雨雪，建议选择室内约会场所，如咖啡馆、电影院、美术馆等 ☔";
        } else if (temp > 30) {
            return "天气较热，建议选择室内有空调的场所，或傍晚再出门 🌞";
        } else if (temp < 10) {
            return "天气较冷，注意保暖，可以安排火锅、温泉等暖身项目 🧥";
        } else if (weather.contains("晴")) {
            return "天气晴朗！非常适合户外约会，公园、爬山、野餐都是好选择 🌟";
        } else if (weather.contains("云")) {
            return "多云天气，不晒不热，适合散步、骑车等户外活动 ☁️";
        } else {
            return "天气条件不错，可以按原计划进行约会 💗";
        }
    }

    /**
     * 降级方案：当 API 调用失败时使用
     */
    private String getFallbackWeather(String city) {
        return String.format("""
            ☀️ **%s 天气参考**
            
            暂时无法获取实时天气数据，建议你：
            1. 打开手机天气 App 查看当前天气
            2. 晴天☀️ → 适合户外约会（公园、散步）
            3. 雨天🌧️ → 选择室内场所（咖啡馆、电影院）
            4. 记得根据温度适当添减衣物
            
            祝你们约会愉快！💕
            """, city);
    }
}