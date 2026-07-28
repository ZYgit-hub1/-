package com.huadianguangdong.collector.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WeatherDataParser 单元测试
 * <p>
 * 验证 JSON 解析、厂区映射、异常值清洗逻辑。
 * 清洗阈值通过 WeatherCleaningProperties 注入，测试可配置性。
 *
 * @author huadianguangdong
 */
@DisplayName("气象数据解析与清洗测试")
class WeatherDataParserTest {

    private WeatherDataParser parser;
    private WeatherCleaningProperties cleaningProps;

    @BeforeEach
    void setUp() {
        cleaningProps = new WeatherCleaningProperties();
        parser = new WeatherDataParser(new ObjectMapper(), cleaningProps);
    }

    @Nested
    @DisplayName("JSON 解析")
    class JsonParsing {

        @Test
        @DisplayName("正常 JSON 解析为 DTO 列表")
        void shouldParseValidJson() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {
                          "stationCode": "GD001",
                          "city": "汕头",
                          "temp": "28.5",
                          "humidity": "75",
                          "windSpeed": "5.2",
                          "windDir": "180",
                          "windDirection": "南风",
                          "rain1h": "0.0",
                          "pressure": "1013.2",
                          "observeTime": "2026-07-27 10:00:00"
                        }
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);

            assertEquals(1, result.size());
            WeatherDataDTO dto = result.get(0);
            assertEquals(1L, dto.getPlantId());
            assertEquals("440500", dto.getDistrictCode());
            assertEquals(28.5f, dto.getTemp());
            assertEquals(75.0f, dto.getHumidity());
            assertEquals(5.2f, dto.getWindSpeed());
            assertEquals((short) 180, dto.getWindDir());
            assertEquals("南风", dto.getWindDirection());
        }

        @Test
        @DisplayName("API 返回非 200 状态码时返回空列表")
        void shouldReturnEmptyWhenApiError() {
            String json = """
                    {"code": 500, "message": "internal error"}
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("空 JSON 返回空列表")
        void shouldReturnEmptyForNullJson() {
            assertTrue(parser.parse(null).isEmpty());
            assertTrue(parser.parse("").isEmpty());
            assertTrue(parser.parse("   ").isEmpty());
        }

        @Test
        @DisplayName("非法 JSON 返回空列表不抛异常")
        void shouldReturnEmptyForInvalidJson() {
            List<WeatherDataDTO> result = parser.parse("{invalid json}");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("厂区代码映射")
    class PlantMapping {

        @Test
        @DisplayName("未知厂区代码的记录被跳过")
        void shouldSkipUnknownStationCode() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "UNKNOWN", "city": "未知城市", "temp": "25.0"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("厂区代码不存在时按城市名回退映射")
        void shouldFallbackToCityMapping() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "UNKNOWN", "city": "广州", "temp": "30.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertEquals(1, result.size());
            // 广州对应第一个匹配的电厂（GD002，plantId=2）
            assertEquals(2L, result.get(0).getPlantId());
            assertEquals("440100", result.get(0).getDistrictCode());
        }
    }

    @Nested
    @DisplayName("异常值清洗（默认阈值）")
    class DataCleaning {

        @Test
        @DisplayName("温度 > 60℃ 标记为 null")
        void shouldCleanAbnormalTemp() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "temp": "65.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertEquals(1, result.size());
            assertNull(result.get(0).getTemp(), "温度 65℃ 应被清洗为 null");
        }

        @Test
        @DisplayName("温度 < -40℃ 标记为 null")
        void shouldCleanAbnormalLowTemp() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "temp": "-50.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getTemp());
        }

        @Test
        @DisplayName("湿度 > 100 标记为 null")
        void shouldCleanAbnormalHumidity() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "humidity": "150.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getHumidity());
        }

        @Test
        @DisplayName("风速 > 62 m/s 标记为 null")
        void shouldCleanAbnormalWindSpeed() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "windSpeed": "80.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getWindSpeed());
        }

        @Test
        @DisplayName("风向 > 359° 标记为 null")
        void shouldCleanAbnormalWindDir() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "windDir": "400", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getWindDir());
        }

        @Test
        @DisplayName("边界值（60℃ / 100%湿度 / 359°风向）保留")
        void shouldKeepBoundaryValues() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {
                          "stationCode": "GD001", "city": "汕头",
                          "temp": "60.0", "humidity": "100", "windDir": "359",
                          "observeTime": "2026-07-27 10:00:00"
                        }
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertEquals(1, result.size());
            assertEquals(60.0f, result.get(0).getTemp());
            assertEquals(100.0f, result.get(0).getHumidity());
            assertEquals((short) 359, result.get(0).getWindDir());
        }

        @Test
        @DisplayName("非数字字段标记为 null")
        void shouldCleanNonNumericValue() {
            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "temp": "abc", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getTemp());
        }
    }

    @Nested
    @DisplayName("清洗阈值可配置性")
    class ConfigurableCleaning {

        @Test
        @DisplayName("自定义温度上限 50℃ 时，55℃ 被清洗为 null")
        void shouldRespectCustomTempMax() {
            // 自定义阈值：温度上限改为 50℃
            cleaningProps.setTempMax(50.0f);

            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "temp": "55.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertEquals(1, result.size());
            assertNull(result.get(0).getTemp(), "自定义上限 50℃ 后，55℃ 应被清洗");
        }

        @Test
        @DisplayName("enabled=false 时保留原始值不清洗")
        void shouldKeepRawValueWhenDisabled() {
            cleaningProps.setEnabled(false);

            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "temp": "99.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertEquals(1, result.size());
            assertEquals(99.0f, result.get(0).getTemp(), "清洗禁用后应保留原始值 99.0");
        }

        @Test
        @DisplayName("自定义湿度上限 80% 时，90% 被清洗为 null")
        void shouldRespectCustomHumidityMax() {
            cleaningProps.setHumidityMax(80.0f);

            String json = """
                    {
                      "code": 200,
                      "data": [
                        {"stationCode": "GD001", "city": "汕头", "humidity": "90.0", "observeTime": "2026-07-27 10:00:00"}
                      ]
                    }
                    """;

            List<WeatherDataDTO> result = parser.parse(json);
            assertNull(result.get(0).getHumidity(), "自定义湿度上限 80% 后，90% 应被清洗");
        }
    }
}
