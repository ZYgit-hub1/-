import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/power_plant.dart';
import '../providers/power_plant_provider.dart';
import '../widgets/warning_color_block.dart';

class PowerPlantDetailPage extends StatelessWidget {
  final String plantId;

  const PowerPlantDetailPage({
    super.key,
    required this.plantId,
  });

  @override
  Widget build(BuildContext context) {
    final plant = context.watch<PowerPlantProvider>().getById(plantId);
    final theme = Theme.of(context);

    if (plant == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('详情')),
        body: const Center(child: Text('未找到该电厂数据')),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(plant.name),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  plant.name,
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              WarningColorBlock(
                level: plant.warningLevel,
                showLabel: true,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            plant.location,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: _DetailMetricCard(
                  icon: Icons.water_drop_outlined,
                  title: '水位',
                  value: '${plant.waterLevel.toStringAsFixed(1)} m',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _DetailMetricCard(
                  icon: Icons.thermostat_outlined,
                  title: '温度',
                  value: '${plant.temperature.toStringAsFixed(1)} °C',
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          Text(
            '运行说明',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            plant.description,
            style: theme.textTheme.bodyLarge?.copyWith(height: 1.5),
          ),
          const SizedBox(height: 24),
          Text(
            '预警说明',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _warningHint(plant.warningLevel),
            style: theme.textTheme.bodyLarge?.copyWith(height: 1.5),
          ),
          const SizedBox(height: 24),
          Text(
            '更新时间：${_formatTime(plant.updatedAt)}',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  String _warningHint(WarningLevel level) {
    switch (level) {
      case WarningLevel.normal:
        return '各项指标处于正常区间，持续常规监测即可。';
      case WarningLevel.caution:
        return '水位或温度偏高，建议加强巡检频率。';
      case WarningLevel.warning:
        return '已接近预警阈值，请核实传感器数据并评估调度方案。';
      case WarningLevel.critical:
        return '指标超过安全阈值，请立即启动应急响应流程。';
    }
  }

  String _formatTime(DateTime time) {
    final y = time.year.toString().padLeft(4, '0');
    final m = time.month.toString().padLeft(2, '0');
    final d = time.day.toString().padLeft(2, '0');
    final h = time.hour.toString().padLeft(2, '0');
    final min = time.minute.toString().padLeft(2, '0');
    return '$y-$m-$d $h:$min';
  }
}

class _DetailMetricCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;

  const _DetailMetricCard({
    required this.icon,
    required this.title,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: theme.colorScheme.primary),
          const SizedBox(height: 10),
          Text(
            title,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}
