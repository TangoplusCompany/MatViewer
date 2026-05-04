plugins {
	alias(libs.plugins.android.application)
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
	implementation("com.google.mediapipe:tasks-vision:0.10.26")
	implementation(libs.androidx.cardview)
	implementation(libs.androidx.recyclerview)
	val camera_version = "1.6.0"
	implementation("androidx.camera:camera-core:$camera_version")
	implementation("androidx.camera:camera-camera2:$camera_version")
	implementation("androidx.camera:camera-lifecycle:$camera_version")
	implementation("androidx.camera:camera-video:$camera_version")
	implementation("androidx.camera:camera-extensions:$camera_version")
	implementation("androidx.camera:camera-view:$camera_version")

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}