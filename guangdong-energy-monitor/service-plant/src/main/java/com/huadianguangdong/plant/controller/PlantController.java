package com.huadianguangdong.plant.controller;

import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.exception.BusinessException;
import com.huadianguangdong.plant.entity.Plant;
import com.huadianguangdong.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电厂 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "电厂管理", description = "电厂的增删改查及空间检索")
@RestController
@RequestMapping("/api/plants")
public class PlantController {

    @Autowired
    private PlantService plantService;

    @Operation(summary = "分页查询电厂")
    @GetMapping
    public R<PageResult<Plant>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String keyword) {
        return R.ok(plantService.page(page, size, type, status, keyword));
    }

    @Operation(summary = "根据 ID 查询电厂")
    @GetMapping("/{id}")
    public R<Plant> getById(@PathVariable Long id) {
        Plant plant = plantService.getById(id);
        if (plant == null) {
            throw new BusinessException(404, "电厂不存在: " + id);
        }
        return R.ok(plant);
    }

    @Operation(summary = "新增电厂")
    @PostMapping
    public R<Plant> create(@Valid @RequestBody Plant plant) {
        plantService.save(plant);
        return R.ok(plant);
    }

    @Operation(summary = "更新电厂")
    @PutMapping("/{id}")
    public R<Plant> update(@PathVariable Long id, @Valid @RequestBody Plant plant) {
        plant.setId(id);
        boolean ok = plantService.updateById(plant);
        if (!ok) {
            throw new BusinessException(404, "电厂不存在: " + id);
        }
        return R.ok(plant);
    }

    @Operation(summary = "删除电厂")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean ok = plantService.removeById(id);
        if (!ok) {
            throw new BusinessException(404, "电厂不存在: " + id);
        }
        return R.ok();
    }

    @Operation(summary = "查询指定坐标半径范围内的电厂")
    @GetMapping("/nearby")
    public R<List<Plant>> nearby(@RequestParam double lng,
                                 @RequestParam double lat,
                                 @RequestParam double radius) {
        if (radius <= 0) {
            throw new BusinessException("半径必须大于 0");
        }
        return R.ok(plantService.listNearby(lng, lat, radius));
    }
}
