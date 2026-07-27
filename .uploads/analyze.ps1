Add-Type -AssemblyName System.Drawing
$src = 'f:\html\src\assets\guangdong-map.png'
$img = [System.Drawing.Image]::FromFile($src)
$w = $img.Width  # 1024
$h = $img.Height # 837
$ratio = $w / $h  # 1.223

# 当前投影函数: xPct = 6 + ((lng - 109.5) / 8.5) * 88, yPct = 6 + ((26.0 - lat) / 6.2) * 88
# 但图片用 object-fit: contain, 实际渲染区域小于容器
# 需要确定: 图片内容在容器中的实际像素范围

# 假设容器宽高比 != 图片宽高比, contain 模式下:
# 如果容器更"高"(宽高比小), 图片按宽度铺满, 上下留白
# 如果容器更"宽"(宽高比大), 图片按高度铺满, 左右留白

# 当前容器 489x500 (宽高比0.978), 图片 1.223 -> 图片更宽, 按宽度铺满
# 渲染宽度 = 489, 渲染高度 = 489/1.223 = 399.8
# 上下留白 = (500 - 399.8)/2 = 50.1

# 所以百分比坐标需要转换:
# 实际图片内 x_pixel = xPct% * 489  (宽度铺满, x无需调整)
# 实际图片内 y_pixel = 50.1 + yPct% * 500 * (399.8/500)
# 即 y_in_image_pct = (yPct% * 500 - 50.1) / 399.8

# 但更根本的问题: 当前 MAP_BOUNDS (lng 109.5-118, lat 19.8-26) 映射到 6%-94%
# 这个范围是否匹配图片实际地理范围? 需要校准

$img.Dispose()
"WIDTH=$w"
"HEIGHT=$h"
"RATIO=$ratio"
