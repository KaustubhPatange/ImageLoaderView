# ImageLoaderView

![build](https://github.com/KaustubhPatange/ImageLoaderView/workflows/build/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.kaustubhpatange/imageloaderview)

An `ImageView` with cool loading effects eg: Shimmer. I had built something similar for my client, this one is the complete version of the prototype.

<img src="art/demo.gif" height="400px">

### Why you need this?

Having an effect (shown in gif) requires multiple views to be laid out. For eg: to achieve the Shimmer effect with this [library](https://github.com/facebook/shimmer-android/), you can wrap your `ImageView` inside the `ShimmerFrameLayout` (which it provides) & also the overlay `ImageView` (with circular data-usage icon), you already took 3 layout passes to draw the same thing. In places like `RecyclerView` where each draw calls are expensive we must optimize it.

This view draws everything in one view & also manage the animation states which would unnecessarily increase boiler-plate if done manually.

## Usage

The repository contains the [sample](sample/) project which is basically the above demo (gif) in action.

```xml
<com.kpstv.imageloaderview.ImageLoaderView
    android:id="@+id/imageloaderview"
    android:layout_width="..."
    android:layout_height="..." />
```

**Create a shimmer load effect**

```xml
<com.kpstv.imageloaderview.ImageLoaderView
   ...
   app:shimmering="true"
   app:corner_radius="10dp"
   app:overlay_drawable="@drawable/..."
   app:overlay_drawable_tint="?attr/colorControlNormal" />
```

- Once your image is loaded call any of the `setImage**` methods with `animate` property to `true`, see example [here](https://github.com/KaustubhPatange/ImageLoaderView/blob/878a975a88e5367eb6fcec85db41ef5598486596/sample/src/main/java/com/kpstv/imageloaderview_sample/MainActivity.kt#L31-L32).

## License

- [The Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)

```
Copyright 2020 Kaustubh Patange

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
