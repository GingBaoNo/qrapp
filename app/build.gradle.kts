plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services") version "4.4.2" apply false
}

android {
    namespace = "com.example.qrapp"
    compileSdk = 35 // Android 15 (UpsideDownCake), phiên bản mới nhất

    defaultConfig {
        applicationId = "com.example.qrapp"
        minSdk = 28 // Tốt cho ứng dụng hiện đại. Cần cân nhắc nếu bạn cần hỗ trợ các thiết bị cũ hơn
        targetSdk = 34 // Target Android 14 (U), phiên bản ổn định gần nhất

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Kích hoạt View Binding để dễ dàng truy cập các view trong Activity/Fragment
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core Libraries - Sử dụng phiên bản từ libs.versions.toml của bạn
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // Thêm core-ktx nếu bạn đang dùng Kotlin hoặc muốn các tiện ích mở rộng Kotlin
    // implementation(libs.core.ktx) // Ví dụ nếu bạn có libs.core.ktx = "androidx.core:core-ktx:1.13.1"

    // ML Kit Barcode Scanning - Sử dụng phiên bản ổn định mới nhất
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // QR Code Generator (nếu bạn cần tạo QR code)
    implementation("com.github.androidmads:QRGenerator:1.0.5")

    // CameraX - ĐỒNG BỘ TẤT CẢ VỀ CÙNG MỘT PHIÊN BẢN ỔN ĐỊNH
    val cameraxVersion = "1.3.3" // CẬP NHẬT PHIÊN BẢN ỔN ĐỊNH MỚI NHẤT TẠI ĐÂY (tháng 5/2025)
    // Kiểm tra: https://developer.android.com/jetpack/androidx/releases/camera

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // Google Guava (quan trọng cho ListenableFuture và các tiện ích đồng bộ)
    // Loại bỏ dòng trùng lặp bạn đã có
    implementation("com.google.guava:guava:30.1-android")


    // Để hiển thị hình ảnh tròn
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Firebase Crashlytics Buildtools - Thường chỉ dùng khi setup Crashlytics.
    // Nếu bạn đang cấu hình Crashlytics, bạn sẽ cần các thư viện Firebase khác nữa.
    // Ví dụ: implementation 'com.google.firebase:firebase-crashlytics-ktx'
    // và implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation(libs.firebase.crashlytics.buildtools)


    // Testing Libraries - Sử dụng phiên bản từ libs.versions.toml của bạn
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // (Tùy chọn) Nếu bạn đã sử dụng zxing-android-embedded trước đó nhưng đã chuyển sang ML Kit
    // và không còn cần nó, hãy bỏ comment hoặc xóa dòng này.
    // Nếu bạn muốn giữ lại cho các tính năng đặc biệt của zxing, hãy giữ lại.
     implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // Room components
    // room-runtime: Chứa các lớp cốt lõi để làm việc với Room
    implementation ("androidx.room:room-runtime:2.6.1")
    // room-compiler: Được sử dụng bởi Annotation Processor để tạo ra code cần thiết cho database
    annotationProcessor ("androidx.room:room-compiler:2.6.1")

    // Optional: Để tích hợp LiveData và ViewModel (thường dùng với Room)
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

    // Optional: Để sử dụng RecyclerView để hiển thị dữ liệu
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    // Optional: Thư viện Glide để tải ảnh hiệu quả (nếu bạn có lưu đường dẫn ảnh)
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
}