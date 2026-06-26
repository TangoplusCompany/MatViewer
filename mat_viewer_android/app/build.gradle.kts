plugins {
	alias(libs.plugins.android.application)
	id("com.google.devtools.ksp")

}
android {
	namespace = "com.tangoplus.matviewer"
	compileSdk {
		version = release(36) {
			minorApiLevel = 1
		}
	}

	defaultConfig {
		applicationId = "com.tangoplus.matviewer"
		minSdk = 31
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	viewBinding {
		enable = true
	}
}

dependencies {
	implementation("com.github.mik3y:usb-serial-for-android:3.9.0")
	implementation("io.github.jan-tennert.supabase:storage-kt:3.0.3")
	implementation("io.ktor:ktor-client-android:3.5.0")
	implementation("io.ktor:ktor-client-logging:3.5.0")
	implementation("io.ktor:ktor-client-content-negotiation:3.5.0")
	implementation("com.google.zxing:core:3.5.4")
	implementation("com.google.android.gms:play-services-location:21.3.0")
	implementation(libs.androidx.recyclerview)
	val roomVersion = "2.7.1" // 구형 환경 및 최신 환경 모두 호환성이 가장 좋은 안정 버전입니다.
	implementation("androidx.room:room-runtime:$roomVersion")
	implementation("androidx.room:room-ktx:$roomVersion")
	implementation("androidx.room:room-rxjava2:$roomVersion")
	ksp("androidx.room:room-compiler:$roomVersion")

	implementation(libs.androidx.cardview)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.fragment.ktx)
	implementation(libs.androidx.constraintlayout)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}