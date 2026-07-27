import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/power_plant_provider.dart';
import '../widgets/power_plant_list_item.dart';
import 'power_plant_detail_page.dart';

class PowerPlantListPage extends StatelessWidget {
  const PowerPlantListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('电厂监测'),
        centerTitle: true,
      ),
      body: Consumer<PowerPlantProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading && provider.plants.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }

          if (provider.error != null && provider.plants.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(provider.error!),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: provider.loadPlants,
                    child: const Text('重新加载'),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: provider.refresh,
            child: ListView.separated(
              physics: const AlwaysScrollableScrollPhysics(),
              itemCount: provider.plants.length,
              separatorBuilder: (_, __) => const Divider(height: 1, indent: 38),
              itemBuilder: (context, index) {
                final plant = provider.plants[index];
                return PowerPlantListItem(
                  plant: plant,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => PowerPlantDetailPage(plantId: plant.id),
                      ),
                    );
                  },
                );
              },
            ),
          );
        },
      ),
    );
  }
}
