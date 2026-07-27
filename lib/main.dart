import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'pages/power_plant_list_page.dart';
import 'providers/power_plant_provider.dart';

void main() {
  runApp(const PowerPlantApp());
}

class PowerPlantApp extends StatelessWidget {
  const PowerPlantApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => PowerPlantProvider(),
      child: MaterialApp(
        title: '电厂监测',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF1565C0),
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          appBarTheme: const AppBarTheme(
            scrolledUnderElevation: 0,
          ),
        ),
        home: const PowerPlantListPage(),
      ),
    );
  }
}
