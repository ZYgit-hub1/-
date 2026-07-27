import 'dart:math';

import 'package:flutter/foundation.dart';

import '../models/power_plant.dart';

class PowerPlantProvider extends ChangeNotifier {
  final Random _random = Random();

  List<PowerPlant> _plants = [];
  bool _isLoading = false;
  String? _error;

  List<PowerPlant> get plants => List.unmodifiable(_plants);
  bool get isLoading => _isLoading;
  String? get error => _error;

  PowerPlantProvider() {
    loadPlants();
  }

  PowerPlant? getById(String id) {
    try {
      return _plants.firstWhere((p) => p.id == id);
    } catch (_) {
      return null;
    }
  }

  Future<void> loadPlants() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      await Future.delayed(const Duration(milliseconds: 800));
      _plants = _generateMockData();
    } catch (e) {
      _error = '加载失败，请下拉重试';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> refresh() async {
    _error = null;
    try {
      await Future.delayed(const Duration(milliseconds: 1000));
      _plants = _generateMockData();
      notifyListeners();
    } catch (e) {
      _error = '刷新失败，请重试';
      notifyListeners();
    }
  }

  List<PowerPlant> _generateMockData() {
    final now = DateTime.now();
    final base = [
      (
        id: '1',
        name: '三峡水电站',
        location: '湖北宜昌',
        description: '长江干流关键控制性水利枢纽，承担防洪、发电、航运等综合任务。',
      ),
      (
        id: '2',
        name: '白鹤滩水电站',
        location: '四川宁南 / 云南巧家',
        description: '金沙江下游梯级电站，单机容量与总装机均居世界前列。',
      ),
      (
        id: '3',
        name: '溪洛渡水电站',
        location: '四川雷波 / 云南永善',
        description: '金沙江下游梯级电站之一，兼具防洪与发电功能。',
      ),
      (
        id: '4',
        name: '乌东德水电站',
        location: '四川会东 / 云南禄劝',
        description: '金沙江下游四座梯级电站中的第一级。',
      ),
      (
        id: '5',
        name: '向家坝水电站',
        location: '四川宜宾 / 云南水富',
        description: '金沙江下游最末一级梯级电站。',
      ),
      (
        id: '6',
        name: '龙滩水电站',
        location: '广西天峨',
        description: '红水河梯级开发的龙头电站，调节性能良好。',
      ),
      (
        id: '7',
        name: '二滩水电站',
        location: '四川攀枝花',
        description: '雅砻江下游大型水电站，对西南电网具有重要调节作用。',
      ),
      (
        id: '8',
        name: '糯扎渡水电站',
        location: '云南普洱',
        description: '澜沧江中下游梯级电站中的龙头水库电站。',
      ),
    ];

    return base.map((item) {
      final waterLevel = 80 + _random.nextDouble() * 100;
      final temperature = 8 + _random.nextDouble() * 28;
      return PowerPlant(
        id: item.id,
        name: item.name,
        waterLevel: double.parse(waterLevel.toStringAsFixed(1)),
        temperature: double.parse(temperature.toStringAsFixed(1)),
        warningLevel: _resolveWarning(waterLevel, temperature),
        location: item.location,
        description: item.description,
        updatedAt: now.subtract(Duration(minutes: _random.nextInt(120))),
      );
    }).toList();
  }

  WarningLevel _resolveWarning(double waterLevel, double temperature) {
    if (waterLevel > 165 || temperature > 32) {
      return WarningLevel.critical;
    }
    if (waterLevel > 150 || temperature > 28) {
      return WarningLevel.warning;
    }
    if (waterLevel > 135 || temperature > 24) {
      return WarningLevel.caution;
    }
    return WarningLevel.normal;
  }
}
