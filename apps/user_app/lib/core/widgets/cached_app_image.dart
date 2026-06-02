import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../network/app_api_client.dart';

class CachedAppImage extends StatelessWidget {
  const CachedAppImage({
    super.key,
    required this.imageUrl,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    required this.placeholder,
  });

  final String? imageUrl;
  final double? width;
  final double? height;
  final BoxFit fit;
  final Widget placeholder;

  @override
  Widget build(BuildContext context) {
    final image = imageUrl?.trim();
    if (image == null || image.isEmpty) return placeholder;
    return CachedNetworkImage(
      imageUrl: AppApiClient.resolvePublicUrl(image),
      width: width,
      height: height,
      fit: fit,
      placeholder: (_, _) => placeholder,
      errorWidget: (_, _, _) => placeholder,
    );
  }
}
