# تعليمات الإعداد النهائي

## الملفات المطلوبة للإكمال

### 1. أيقونات التطبيق
- `app/src/main/res/mipmap-*/ic_launcher.png` - أيقونة التطبيق
- `app/src/main/res/mipmap-*/ic_launcher_round.png` - أيقونة التطبيق المدورة

### 2. الخطوط العربية (اختيارية)
- `app/src/main/res/font/arabic_regular.ttf` - خط عربي عادي
- `app/src/main/res/font/arabic_bold.ttf` - خط عربي عريض

## خطوات الإعداد

### 1. إعداد Android Studio
```bash
cd SpaceNewsApp
./gradlew clean
./gradlew build
```

### 2. إعداد مصادر RSS (اختيارية)
في `NewsRepository.kt`، يمكنك إضافة مصادر RSS إضافية:
```kotlin
private val rssUrls = listOf(
    "https://www.space.com/feeds/all",
    "https://www.sciencedaily.com/rss/space_time.xml",
    "https://spacegroup.com.qa/apk/app/rss.txt" // إذا كان متاحاً
)
```

### 3. إعداد الخطوط (اختيارية)
إذا كنت تريد استخدام خطوط عربية مخصصة:
1. ضع ملفات الخطوط في `app/src/main/res/font/`
2. تأكد من أن أسماء الملفات تطابق ما في `font_family.xml`

### 4. إعداد الأيقونات
1. أنشئ أيقونة للتطبيق بأحجام مختلفة
2. ضعها في المجلدات المناسبة في `mipmap-*`

### 5. اختبار التطبيق
1. تشغيل على جهاز أو محاكي
2. تفعيل التنبيهات
3. اختبار جلب الأخبار
4. اختبار المفضلة والمشاركة

## المميزات المتوفرة

✅ جلب الأخبار من RSS
✅ حفظ الأخبار محلياً
✅ نظام المفضلة
✅ التنبيهات في الخلفية
✅ مشاركة الأخبار
✅ تصميم جميل ومتجاوب
✅ دعم العربية
✅ وضع القراءة/عدم القراءة

## التخصيص

### تغيير الألوان
في `colors.xml`:
```xml
<color name="primary_color">#1A237E</color>
<color name="accent_color">#FF5722</color>
```

### تغيير فترة التحديث
في `NewsWorker.kt`:
```kotlin
repeatInterval = 30, // دقيقة
```

### إضافة مصادر RSS
في `NewsRepository.kt`:
```kotlin
private val rssUrls = listOf(
    "URL_الجديد_هنا"
)
```

## المشاكل المحتملة

### 1. عدم ظهور الخطوط العربية
- تأكد من وجود ملفات الخطوط في المجلد الصحيح
- أو احذف المراجع للخطوط المخصصة من `styles.xml`

### 2. عدم عمل التنبيهات
- تأكد من منح إذن التنبيهات في إعدادات الجهاز
- تفعيل التنبيهات في التطبيق

### 3. عدم جلب الأخبار
- تحقق من اتصال الإنترنت
- تأكد من صحة روابط RSS

## الدعم الفني

إذا واجهت أي مشاكل:
1. تحقق من Android Studio Logcat
2. تأكد من إعدادات الشبكة
3. راجع ملف `build.gradle` للتبعيات

---

التطبيق جاهز للاستخدام! 🚀