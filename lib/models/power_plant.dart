class PowerPlant {
  final String id;
  final String name;
  final double waterLevel;
  final double temperature;
  final WarningLevel warningLevel;
  final String location;
  final String description;
  final DateTime updatedAt;

  const PowerPlant({
    required this.id,
    required this.name,
    required this.waterLevel,
    required this.temperature,
    required this.warningLevel,
    required this.location,
    required this.description,
    required this.updatedAt,
  });

  PowerPlant copyWith({
    String? id,
    String? name,
    double? waterLevel,
    double? temperature,
    WarningLevel? warningLevel,
    String? location,
    String? description,
    DateTime? updatedAt,
  }) {
    return PowerPlant(
      id: id ?? this.id,
      name: name ?? this.name,
      waterLevel: waterLevel ?? this.waterLevel,
      temperature: temperature ?? this.temperature,
      warningLevel: warningLevel ?? this.warningLevel,
      location: location ?? this.location,
      description: description ?? this.description,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

enum WarningLevel {
  normal,
  caution,
  warning,
  critical,
}

extension WarningLevelX on WarningLevel {
  String get label {
    switch (this) {
      case WarningLevel.normal:
        return '正常';
      case WarningLevel.caution:
        return '关注';
      case WarningLevel.warning:
        return '预警';
      case WarningLevel.critical:
        return '危险';
    }
  }
}
