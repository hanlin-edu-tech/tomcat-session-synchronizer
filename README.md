# tomcat-session-synchronizer

## 使用方法
```groovy
repositories {
    maven { url "gcs://ehanlin-java-repo/maven2" }
}

dependencies {
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-core:1.0.0"
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-tomcat8:1.0.0"
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-mongodb:1.0.0"
}
```

## 佈署
```shell
./gradlew -Pversion=1.0.0 publish
```