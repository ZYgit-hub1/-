import 'package:flutter/material.dart';

import '../models/power_plant.dart';

class WarningColorBlock extends StatelessWidget {
  final WarningLevel level;
  final double width;
  final double height;
  final bool showLabel;

  const WarningColorBlock({
    super.key,
    required this.level,
    this.width = 8,
    this.height = 56,
    this.showLabel = false,
  });

  Color get color {
    switch (level) {
      case WarningLevel.normal:
        return const Color(0xFF2E7D32);
      case WarningLevel.caution:
        return const Color(0xFFF9A825);
      case WarningLevel.warning:
        return const Color(0xFFEF6C00);
      case WarningLevel.critical:
        return const Color(0xFFC62828);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (showLabel) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: color.withOpacity(0.15),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: color, width: 1.2),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                color: color,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 6),
            Text(
              level.label,
              style: TextStyle(
                color: color,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(4),
      ),
    );
  }
}
