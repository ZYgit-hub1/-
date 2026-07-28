package com.huadianguangdong.plant.controller;

import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.exception.BusinessException;
import com.huadianguangdong.plant.entity.HydroStation;
import com.huadianguangdong.plant.service.HydroStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 水文站 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "水文站管理", description = "水文站的查询管理")
@RestController
@RequestMapping("/api/hydro/stations")
public class HydroStationController {

    @Autowired
    private HydroStationService hydroStationService;

    @Operation(summary = "分页查询水文站")
    @GetMapping
    public R<PageResult<HydroStation>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) String river,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(hydroStationService.page(page, size, city, river, keyword));
    }

    @Operation(summary = "根据 ID 查询水文站")
    @GetMapping("/{id}")
    public R<HydroStation> getById(@PathVariable Long id) {
        HydroStation station = hydroStationService.getById(id);
        if (station == null) {
            throw new BusinessException(404, "水文站不存在: " + id);
        }
        return R.ok(station);
    }

    @Operation(summary = "按城市查询水文站")
    @GetMapping("/by-city/{city}")
    public R<List<HydroStation>> listByCity(@PathVariable String city) {
        return R.ok(hydroStationService.listByCity(city));
    }

    @Operation(summary = "按河流查询水文站")
    @GetMapping("/by-river/{river}")
    public R<List<HydroStation>> listByRiver(@PathVariable String river) {
        return R.ok(hydroStationService.listByRiver(river));
    }

    @Operation(summary = "新增水文站")
    @PostMapping
    public R<HydroStation> create(@Valid @RequestBody HydroStation station) {
        hydroStationService.save(station);
        return R.ok(station);
    }
}
