package com.huadianguangdong.analysis.controller;

import com.huadianguangdong.analysis.predict.PredictService;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预测 Controller
 *
 * <p>对接 Python 预测服务，对外暴露水位预测与发电预测接口。
 * 支持自定义预测时长和模型类型。
 *
 * @author huadianguangdong
 */
@Tag(name = "预测服务", description = "水位预测 / 发电预测（含降级 + TDengine 持久化 + 趋势预警）")
@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictController {

    private final PredictService predictService;

    /**
     * 水位预测。
     *
     * @param stationId     水文站 ID
     * @param forecastHours 预测未来时长（小时，默认 24）
     * @param modelType     模型类型（lstm / xgboost / arima，默认 lstm）
     * @return 预测结果
     */
    @Operation(summary = "水位预测", description = "调用 Python 预测服务，降级时使用本地 ARIMA")
    @GetMapping("/water-level/{stationId}")
    public R<PredictionResponseDTO> waterLevel(
            @Parameter(description = "水文站 ID") @PathVariable Long stationId,
            @Parameter(description = "预测时长（小时）") @RequestParam(defaultValue = "24") Integer forecastHours,
            @Parameter(description = "模型类型") @RequestParam(defaultValue = "lstm") String modelType) {
        return R.ok(predictService.predictWaterLevel(stationId, forecastHours, modelType));
    }

    /**
     * 发电预测。
     *
     * @param plantId       电厂 ID
     * @param forecastHours 预测未来时长（小时，默认 24）
     * @param modelType     模型类型（lstm / xgboost / arima，默认 lstm）
     * @return 预测结果
     */
    @Operation(summary = "发电预测", description = "调用 Python 预测服务，降级时使用本地 ARIMA")
    @GetMapping("/power/{plantId}")
    public R<PredictionResponseDTO> power(
            @Parameter(description = "电厂 ID") @PathVariable Long plantId,
            @Parameter(description = "预测时长（小时）") @RequestParam(defaultValue = "24") Integer forecastHours,
            @Parameter(description = "模型类型") @RequestParam(defaultValue = "lstm") String modelType) {
        return R.ok(predictService.predictPower(plantId, forecastHours, modelType));
    }
}
