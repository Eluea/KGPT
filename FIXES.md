# KGPT — سجل الإصلاحات الكاملة (FIXES.md)

> **المشروع**: KGPT (KeyboardGPT fork) — `tn.eluea.kgpt` v4.1.4
> **إطار العمل**: LSPosed Modern API — `minApiVersion=100, targetApiVersion=102`
> **آخر تحديث**: 2026-08-24 — بعد جلسة إصلاح شاملة غطت 17 مشكلة موثقة في `KGPT_ISSUES.json`

---

## 📌 قيود ثابتة خلال كل الإصلاحات

- كل الحلول متوافقة مع **Modern API 100/102** فقط — لا اعتماد على أي سلوك Legacy (مثل حقن تطبيق الموديل في نفسه، أو meta-data القديمة).
- أي فحص يعتمد على سلوك إطار عمل غير رسمي مرفوض؛ مصادر الحقيقة الرسمية: `XposedService` binding + نبضة الحقن الفعلية.

---

# أولاً: كشف حالة الموديل

## ✅ ISSUE-001 — ظهور "Module Active" خطأً بدون LSPosed
- **العرض**: التطبيق يظهر أنه مفعّل حتى بدون تثبيت LSPosed.
- **السبب الجذري** (مثبت من مصدر AOSP): فحص `isWorldReadableAvailable()` كان يفترض أن عدم رمي `SecurityException` عند استخدام `MODE_WORLD_READABLE` يعني تفعيل LSPosed. لكن في `ContextImpl.getSharedPreferences(File, int)` دالة `checkMode()` تعمل **فقط عند cache miss**، والكاش مفهرس بالملف فقط. وبما أن `ConfigProvider.onCreate()` يعمل قبل الواجهات ويفشل ثم يعيد المحاولة بـ `MODE_PRIVATE` (فيملأ الكاش)، فإن كل الاستدعاءات اللاحقة ترجع النسخة المخبأة **بدون أي استثناء أبداً** → الفحص يرجع true على أي جهاز.
- **الإصلاح**: حذف الفحص من `HomeFragment.checkModuleStatus()` ومن `MediaDownloaderActivity.isLSPosedActiveOnDevice()` + حذف الدالة نهائياً من `WorldReadablePrefs` مع تعليق توثيقي.

## ✅ ISSUE-002 — حالة "Restart Required" كانت كوداً ميتاً
- **السبب**: `module_enabled_time` لا يكتبه أي كود، و`isLSPosedModuleEnabled()` stub يرجع false دائماً. وتحت Modern API لا توجد إشارة push لرصد التفعيل أثناء تشغيل العملية.
- **الإصلاح**: حذف الحالة بالكامل (الثوابت + منطق القراءة + حالة الواجهة) وتبسيط الحالات إلى Active/NotActive.

## ✅ ISSUE-003 — الـ self-hook كود ميت تحت Modern API
- **السبب**: ويكي LSPosed تنص حرفياً *"module apps are no longer hooked by themselves"* — أي منطق يعتمد وصول `onPackageLoaded` لباكدج التطبيق نفسه لن يعمل مع `minApiVersion=100`.
- **الإصلاح**: حذف فرع self-hook من `MainHook.onPackageLoaded` وحذف `isModuleActiveInternal()` من HomeFragment.

## ✅ ISSUE-014 — البطاقة تعرض "غير مفعّل" رغم عمل الهوك (تكميلي)
- **السبب**: الاعتماد على ربط `XposedService` وحده غير كافٍ واقعياً — تسليم الـ binder له سباقات وتوقيتات غير موثوقة (تفعيل بعد الإقلاع/إعادة تثبيت بتوقيع مختلف/restart للدايمن).
- **الإصلاح — نظام إشارتين**:
  1. ربط XposedService (فوري) + 3 محاولات إعادة ربط (3s/8s/20s) في `LSPosedHelper.init`.
  2. **نبضة التفعيل**: `MainHook.ensureInitialized` — الذي يعمل فقط داخل عملية مهوكاة فعلاً — يكتب `module_activation_heartbeat` عبر `ConfigProvider` (أول خطوة قبل أي init قد يفشل). البطاقة = Active إذا: خدمة مربوطة **أو** نبضة عمرها < 24 ساعة.
- **ملاحظة ميدانية مؤكدة بالاختبار**: بعد إعادة تثبيت التطبيق بتوقيع مختلف يوقف LSPosed الحقن فعلياً حتى إعادة تفعيل الموديل يدوياً من المدير — والبطاقة حينها تعرض الحقيقة.
- **تحقق مباشر**:
  ```bash
  adb shell content query --uri content://tn.eluea.kgpt.provider/config/module_activation_heartbeat
  ```

---

# ثانياً: التريجرز والكوماندات

## ✅ ISSUE-005 — التريجرز والكوماندات لا تعمل إلا بعد edit+save يدوي
- **السلسلة**:
  1. `ParsePattern.isEnabled()`: غياب `_enabled` = معطل.
  2. نظام `_enabled` أُضيف في v3.0.2 — البيانات الأقدم بلا extras → كلها disabled، ولا يوجد reseed لأن القيمة موجودة.
  3. migration يعالج الأنواع *المفقودة* فقط وليس الناقص extras.
  4. نمط `$` المعطل يجعل `aiTriggerEnabled=false` → كل الكوماندات (`$cmd$`, `/ask`, inline) تموت.
  5. لا إعادة بناء دورية للـ TextParser في عملية الكيبورد — إعادة البناء الوحيدة كانت بث `DIALOG_RESULT` (أي edit+save يدوي بالضبط).
- **الإصلاح**: غياب `_enabled` = **مفعّل** افتراضياً (استثناء MediaDownloader) + backfill تلقائي في `decode()` + `createDefault()` موحدة + إعادة بناء دورية في `KGPTBrain.reloadConfigIfNeeded()` عبر `reloadPatterns()/reloadCommands()` الجديدة (تعمل فقط عند تغير الـ raw payload).

## ✅ ISSUE-006 — زر Reset يعطّل التريجر صامتةً
- **السبب**: `InvocationPatternsFragment.java` كان ينشئ `new ParsePattern(type, defaultPattern)` بدون extras → `_enabled` مفقود → معطل حسب الدلالات القديمة.
- **الإصلاح**: يستخدم `ParsePattern.createDefault(type)` التي تكتب الحالة صراحة.

---

# ثالثاً: الأداء (Performance)

## ✅ ISSUE-009 — ثقل مسار كل ضغطة مفتاح في عملية الكيبورد
- **الجذور**:
  - `getExtractedText()` لكل ضغطة = نداء Binder ينسخ كامل نص الحقل (O(n²) للرسالة).
  - ترجمة regex جديدة لكل ضغطة في 3 مواقع (ask pattern / inline commands / inline ask).
  - نشر Runnable فحص إعدادات لكل ضغطة.
- **الإصلاح**:
  - قراءة محدودة `getTextBeforeCursor(4000)` بدل استخراج الحقل كاملاً (كل التريجرز suffix-anchored).
  - كاش أحادي المدخل للأنماط الثلاثة (مفتاحه prefix/symbol/قائمة الكوماندات — يتغير فقط عند تغير الإعدادات).
  - فحص الإعدادات مجدول كل 5 ثوانٍ على HandlerThread مستقل بدل الربط بضربات المفاتيح.

## ✅ ISSUE-011 — إعادة فحص DexKit الكامل عند كل Activity في YouTube
- **السبب**: `Activity.onCreate` after-hook كان يشغل `scanAndHookDexKitLayers` (إنشاء DexKitBridge فوق كل DEX + عدة findClass بصمات نصية) بلا أي تخزين مؤقت — مئات المللي ثانية على الثريد الرئيسي لكل شاشة.
- **الإصلاح**: مجموعة `SCANNED_LOADERS` (WeakHashMap-backed) — الفحص مرة واحدة لكل ClassLoader **عند النجاح فقط** (انظر الانحدار أدناه).

### 🔁 الانحدار المرتبط — ISSUE-016 (تم إصلاحه)
- **الخطأ الأول في الإصلاح**: الفحص الأولي وقت تحميل الموديل يتم بـ `context=null` حيث لا يمكن استخراج `libdexkit.so` من APK الموديل — ومع ذلك كان يحرق علامة "تم الفحص" → إعادة محاولة `Application.onCreate` (الحاسمة والناجحة) تُمنع للأبد → **زر التنزيل الأصلي في YouTube/Music توقف عن فتح البوت شيت**.
- **الإصلاح الصحيح**: `scanAndHookDexKitLayers` ترجع boolean (نجاح = DexKit محمّل والفحص نُفذ)؛ `scanOncePerLoader` يسجل النجاح فقط ويسمح بإعادة المحاولة عند الفشل؛ `hookLayer2DownloadClick` الخفيف عاد غير المشروط (كالأصل)؛ خطأ bridge لا يقطع الـ fallbacks.

## ✅ ISSUE-010 — خصوصية: تسجيل نص المستخدم في logcat
- **السبب**: `TextParser` كان يسجل نص المستخدم الحرفي (`parse() called with text: '...'`) عند كل ضغطة بشكل غير مشروط، داخل كل تطبيق مهوكى + `EnableLogs=true` افتراضياً.
- **الإصلاح**: حذف 11 موضع تسجيل لمحتوى الحقل نهائياً + جعل `EnableLogs=false` افتراضياً.

---

# رابعاً: محرك التنزيل (Downloader)

## ✅ ISSUE-008 — جودات فيديو وهمية وهبوط صامت
- **الجذور**: قائمة جودات hardcoded لكل فيديو • `getFormats()` لا تُستدعى إطلاقاً رغم توفرها في المكتبة • أحجام تقديرية مصمتة • مرشح `height<=H` يهبط صامتةً • عملاء `android_music,android,web,mweb` المقيدة (عصر SABR/PO-Token) • retry يعيد نفس الطلب المقيد.
- **الإصلاح**:
  - قائمة ديناميكية من `getFormats()` (فلترة `vcodec!=none`، تجميع بالارتفاع، عرض fps/format_note/**الحجم الحقيقي**) مع fallback ثابت عند عدم توفر الصيغ.
  - اختيار دقيق: `-f <format_id>+bestaudio/<height fallback>/best` مع إعادة استخدام نفس `extractorArgs` التي أنتجت المعلومات (`DownloadOptions.preferredFormatId/extractorArgs`).
  - **تحذير قبل الهبوط**: لو الجودة المطلوبة > أعلى متاح فعلياً → تأكيد "أعلى متاحة هي Xp" (strings ar/en).
  - سلسلة عملاء متدرجة: defaults أولاً → legacy pinned → update+retry، مع توثيق `lastSuccessfulExtractorArgs`.
  - إعادة محاولة ذكية تعيد بناء الطلب بدون التثبيتات بدل تكراره.

## ✅ ISSUE-015 — صوتيات وحاويات غير حقيقية أيضاً
- **قبل**: خيارات صوت ثابتة (MP3 320/256/192/"Lossless FLAC") كلها إعادة ترميز لمصدر ضائع أصلاً، وحاويات فيديو ثابتة حتى لو لم توجد streams مطابقة.
- **الإصلاح**:
  - قائمة صوت ديناميكية: **"أفضل صوت أصلي (بدون إعادة ترميز)"** ثم كل المصادر الفعلية (`Original Opus · ~160 kbps · 57 MB · .webm`) مرتبة بالـ bitrate مع format_id دقيق.
  - `DownloadOptions.keepOriginalAudio`: المحرك يتخطى `--audio-format/--audio-quality` فيحفظ الكوديك الأصلي 1:1.
  - خيارات التحويل بقيت موسومة بصدق بـ "(محوّل)".
  - حاويات فيديو ديناميكية: MP4 دائماً + WebM فقط إن وُجدت streams webm فعلاً + MKV كهدف remux شامل.

## ✅ نظام الكاش الآمن (ISSUE-017) — منع ظهور بيانات فيديو سابق/آخر
- **الجذور الثلاثة**:
  1. طلب التنزيل لم يكن يضيف `--no-cache-dir` (خلافاً لطلب التحليل) → كاش yt-dlp المسوم قد ينتج metadata/formats لفيديو آخر.
  2. `findDownloadedFile()` كانت ترجع **أحدث ملف في المجلد** عمياءً — فينسوب النتيجة لفيديو سابق أو تنزيل متوازٍ.
  3. `clearCache()` كانت تحذف من مجلد الكاش دون النظر لتنزيلات نشطة (خطر إفساد تنزيل جارٍ)، وملفات `.part/.ytdl` القديمة لنفس العنوان قد تُستأنف فاسدة.
- **الإصلاح**:
  - `--no-cache-dir` أصبح جزءاً من طلب التنزيل.
  - `ACTIVE_DOWNLOAD_STARTS` (processId→زمن البدء): `clearCache` الآمنة لا تحذف أي ملف أحدث من أقدم تنزيل نشط (هامش 60 ثانية).
  - `findDownloadedFile(directory, startedAt, template)`: لا تقبل إلا ملفات كُتبت أثناء هذا التشغيل (±2s)، وأسبقية لمن يطابق literal-prefix من قالب الاسم، ثم الأحدث.
  - `cleanupStalePartials()`: قبل كل تنزيل تُحذف `.part/.ytdl` القديمة المطابقة لـ literal prefix لنفس العنوان فقط — لا مساس بملفات فيديوهات أخرى.
  - حارس `loadGeneration` في البوت شيت: نتيجة fetch لفيديو سابق لا يمكنها ملء الشاشة الحالية أبداً (سباق إغلاق/فتح سريع).

---

# خامساً: المتانة والأمان

## ✅ ISSUE-012 — وكيل الإنترنت بين العمليات
- **السبب**: لا DeathRecipient ولا مهلات — لو ماتت عملية KGPT أثناء طلب AI من الكيبورد، قارئ `PipedInputStream` يعلق للأبد ويستنزف تجمع AiResponseManager (ثريدَان). كما كان InternetService ينشئ Thread بلا حد لكل طلب.
- **الإصلاح**: `linkToDeath` + `onServiceUnavailable()` تفشل بكل المعلق فوراً • مهلة 60 ثانية بانتظار status code في `InternetRequestPublisher` (مع `onRequestError` جديد افتراضي no-op في الواجهة) • مهلة 30 ثانية في `ExternalInternetProvider` • `newFixedThreadPool(4)` في الخدمة.

## ✅ ISSUE-013 — تجاوز SSL + cleartext عام
- **الإصلاح**: `handler.cancel()` بدل `proceed()` في WebView البحث (كان MITM exposure) + `network_security_config.xml` جديد: cleartext ممنوع عاماً ومسموح فقط لـ loopback (localhost/127.0.0.1/10.0.2.2 لتغطية مزودات AI المحلية مثل Ollama/LM Studio).

## ⚠️ ISSUE-004 — أمان (جزئي بحكم قيد معماري)
- **منفذ**: خارجنة مفاتيح التوقيع (`keystore.properties` gitignored + مثال + env vars `KGPT_*` + fallback للتوقيع الافتراضي في debug عند غياب credentials) • استبعاد `keyboard_gpt.xml` (مفاتيح API) من cloud-backup وdevice-transfer.
- **موثق دون تغيير**: سطح الـ exported (`ConfigProvider`/`InternetService`/...) متعمد لأن قناة IPC مع العمليات المهوكاة (كيبوردات متنوعة) لا تسمح بـ signature-permission دون كسر الوظيفة.

---

# سادساً: إصلاحات مساندة
- إصلاح كسر ترجمة الاختبارات القديم: `testImplementation io.github.libxposed:api` لـ `ExampleUnitTest.kt`.
- `ApkStructureTest`: يتخطى (skip) بدل الفشل عندما لا يوجد APK مبني، والاسم محدث لـ v4.1.4 (كان مثبتاً على v4.0.8 بمسار Windows).
- `build.gradle`: debug يسقط للتوقيع الافتراضي تلقائياً عند غياب credentials (مع تحذير configuration-time للمطور).

---

# 🧪 حالة التحقق

| البند | النتيجة |
|---|---|
| `compileDebugJavaWithJavac` | BUILD SUCCESSFUL |
| Unit Tests | 12 اختبار — BUILD SUCCESSFUL |
| `assembleDebug` | APK موقّع وسليم (META-INF/xposed سليمة) |
| التثبيت الميداني | Xiaomi 11T (vili) عبر adb — install -r ناجح |
| فحص النبضة المباشر | `adb shell content query --uri content://tn.eluea.kgpt.provider/config/module_activation_heartbeat` |

> ⚠️ **ملاحظة ما بعد التثبيت بتوقيع جديد**: يجب إعادة تفعيل KGPT في مدير LSPosed + التأكد من scope الكيبوردات + إعادة تشغيل الكيبورد مرة واحدة.

---

# 📂 خريطة الملفات المعدلة الرئيسية

```
MainHook.java                     نبضة التفعيل + حذف self-hook
KGPTBrain.java                    إعادة بناء دورية للـ parser + جدولة الإعدادات
HomeFragment.java                 checkModuleStatus ثنائية الإشارة + تنظيف ميت
LSPosedHelper.java                إعادة محاولة ربط الخدمة
IMSController.java                قراءة محدودة بدل getExtractedText
TextParser.java                   حذف تسجيل نص المستخدم + كاش ask-pattern
InlineCommand/InlineAsk factories كاش الأنماط
ParsePattern.java                 دلالات _enabled + createDefault + backfill
CommandManager.java               reloadCommands()
InvocationPatternsFragment.java   Reset عبر createDefault
WorldReadablePrefs.java           حذف الفحص المعطوب
BottomSheetHelper.java            overload allowBlurBehind
WebSearchActivity.java            blur-off + SSL cancel + onRenderProcessGone محكم
DownloaderEngine.java             سلسلة عملاء + keepOriginalAudio + كاش آمن + findDownloadedFile زمني
DownloadOptions.java              preferredFormatId/extractorArgs/keepOriginalAudio
MediaDownloaderBottomSheet.java   جودات/صوت/حاويات حقيقية + تحذير الهبوط + generation guard
AbstractServiceClient/ExternalInternetProvider/InternetRequestPublisher/InternetService
                                  DeathRecipient + مهلات + pool(4)
YouTubeHook.java                  dedup DexKit صحيح مع retry-on-failure
AndroidManifest.xml + res/xml     network_security_config + backup exclusions
app/build.gradle                  خارجنة التوقيع + fallback debug signing
OtherSettingsType.java            EnableLogs=false
```
---

# سابعاً: واجهات البوت شيتات العائمة وأمثلة الكوماندات

## ✅ ISSUE-018 — تكامل المزودات المخصصة + أمثلة تابعة لرمز التريجر

### (أ) بوت شيت Choose Model العائم (€ → Choose Model)
- **قبل**: أيقونة عامة واحدة (`ic_model_default`) لكل المزودين، ولا وجود للمزودات المخصصة أو زر إضافتها — أثناء تطوير ميزة Custom Providers حُدّثت واجهة التطبيق فقط وأُهمل هذا الشيت.
- **بعد**:
  - شعار حقيقي لكل مزود عبر `ProviderLogoHelper.getLogoRes()` (Gemini/ChatGPT/Groq/.../Kimi).
  - قسم **Custom Providers**: كل مزود محفوظ يظهر بشعاره واسمه وحالة اختياره.
  - اختيار مزود مخصص يحفظ عبر `setSelectedProviderType/setSelectedCustomProviderId` ويبث نفس extras تدفق التطبيق (`SELECTED_PROVIDER_TYPE` + `SELECTED_CUSTOM_PROVIDER_ID`) فيتحوّل الهوك مباشرة دون المرور على ConfigureModel.
  - زر **"Add Custom Provider"** يفتح **`AddCustomProviderDialogBox` جديد داخل نفس التدفق العائم مباشرة** (بدون مغادرة الحوار): نفس محرر التطبيق كاملاً — Templates، بطاقات Auth، Test Connection، حفظ + اختيار تلقائي + بث التحديث ثم العودة إلى Choose Model.

### (ب) أمثلة الكوماندات داخل التطبيق
- **قبل**: `getExampleForCommand()` كان يبني الأمثلة بحرف `$` الثابت ومثال /ask كذلك — تغيير رمز التريجر من Patterns لا يؤثر.
- **بعد**: `getCurrentTriggerSymbol()` يقرأ رمز CommandAI الحالي (كاش ينقضى عند تغير JSON) وتستخدمه الأمثلة السبع ومثال /ask، مع `notifyDataSetChanged()` عند الرجوع للتبويب.
---

# ثامناً: الانهيار عند الفتح + شريط التنقل + AI Text Selection

## ✅ ISSUE-019 — انهيار مفاجئ عند فتح التطبيق
- **السبب** (من `adb logcat -b crash`): `HomeFragment.updateStatusCard` يستدعي `getResources()` بعد فصل الـ Fragment — مؤقتات `postDelayed(300/1000)` ومستمع ربط الخدمة الساكن ينطلقان بعد الإغلاق السريع.
- **الإصلاح**: حراسة `isAdded()` في `updateUI`/`updateStatusCard`/`updateSearchEngineInfo`.
- **الاختبار**: إجهاد فتح+BACK×3 وفتح/force-stop×4 → **صفر أعطال** في crash buffer (كان `IllegalStateException` مؤكداً سابقاً).

## ✅ ISSUE-020 — أيقونة Home الشبحية + انحدار اللون/السلاسة
- **الجذران**: تركيبات Lottie تُحمّل async (ضبط الإطار قبل الجهوز يُهمل → يظهر اللون المدمج الأبيض)؛ والإصلاح الأولي بـ post غير محمي كان يصدم `playOnce()` للأيقونة المفعّلة (تبقى داكنة والانزلاق يُقطع).
- **الإصلاح**: `applyNavState` بـ **generation tag** — المؤجل يعمل فقط لنفس التوليدة وفقط إذا كانت الـ composition لم تجهز؛ والأيقونة الهدف مستثناة من التصفير.
- **الاختبار** (تحليل بكسل آلي): الرئيسية مفعّلة → أبيض (249,248,255) وModels داكن (92,95,104)؛ بعد النقر على Models → أبيض على الحبة الزرقاء (74,95,139) والرئيسية داكنة. الحالتان صحيحتان.

## ✅ ISSUE-021 — AI Text Selection: ظهور عشوائي + لا تنفيذ في المكان + تعتيم يخفي التحديد
- **الإصلاحات**:
  1. `isEnabled()` افتراضي **true** (مطابق للتطبيق — كان false فيسبب "تظهر مرة ثم لا تظهر").
  2. حقن لنوعي ActionMode (FLOATING **وPRIMARY**).
  3. `mode.invalidate()` بعد الإضافة لإعادة رسم القائمة.
  4. `goAsync()` في `TextActionReceiver` لإبقاء العملية حية طوال رحلة الـ AI → **الاستبدال يصل في مكان التحديد** دون فتح التطبيق.
  5. إزالة blur/تعتيم نافذة قائمة الإجراءات → **النص المحدد يظل مرئياً** أثناء الاختيار والتنفيذ.
- **ملاحظة تصميمية**: تطبيق KGPT نفسه لا تظهر فيه عناصر الحقن إطلاقاً (Modern API: تطبيق الموديل لا يُهاك) — الحقن للتطبيقات الأخرى المضافة للنطاق.
---

# تاسعاً: التدقيق العميق الشامل (ISSUE-022) — 60+ ملاحظة

تدقيق بثلاثة مسارات متوازية (النواة/IPC، الواجهات، الميزات/الهوكس) + موارد واعتمادات. أبرز ما نُفذ:

## الهوكس (الأخطر)
- **نهاية تكديس الـ interceptors**: `HookManager` يحتفظ بـ `HookHandle` لكل Member؛ إعادة الهوك تستدعي `unhook()` الحقيقي من API 102 أولاً، و`unhook()` يفك الـ hook الأصلي فعلياً (كان يمسح خريطة جافية فقط). هوكات InputConnection تُفك الآن **بالهوية** عبر `lastHookedICMembers` (الـ fuzzy finder قد يعيد Methods من superclass فكانت تتيمة للأبد).
- `KGPTBrain` يُعاد بناؤه في كل إعادة تشغيل كيبورد (كان يموت للأبد بعد أول دورة).
- استثناءات كولباكات الهوك تُسجل بدل الابتلاع الصامت.

## الأمان
- **سلسلة توريد النواة**: SHA-256 إلزامي لكل chunk (حتى resume/local)، رفض الحزم بلا digest، تثبيت mirrors على tag، حذف التحميل من /sdcard، حارس Zip-Slip في النسختين.
- بثوث النصوص: `target_package` echo + فلترة، سقف 20K، رفض actions مزورة.
- WebView: منع intent://redirect بلا fallback آمن، إغلاق file/content access، SafeBrowsing ON، debugging للـ debug فقط.
- FileProvider لم يعد يعرض كل الذاكرة • منع تسجيل محتوى الكليبورد في logcat • منع الترقية الهبوطية في المدقق.

## ميزات كانت وهمية وأصبحت حقيقية
- قالب اسم الملف، المفاتيح الخمسة (burn/recode/chapters/thumbnail)، Extra commands، مجلد SAF (tree URI + نسخ عبر ContentResolver)، زر Open في الإشعار (authority mismatch)، الإشعار العالق، مفاتيح تعطيل هوك يوتيوب، hasMore البحث.

## الواجهة والمنطق
- إلغاء Other Settings يسترجع snapshot (كان يحفظ الملغي!) • catch مركزي للحوارات • توحيد خريطة التنقل • `ModelCatalog` مصدر وحيد (كان الكتالوجان يختلفان وPerplexity مفقود) • فحوصات متماثلة (تكرار/built-ins/رموز) • تعريب • status bar يتبع الوضع الليلي.

## الأداء والدورة الحياة
- النسخ الاحتياطي خارج main thread • كاش parse المزودات • `init()` مرة واحدة • pools محدودة • dedupe الصور • LocaleHelper يكتب عند التغيير فقط • القط كل 3 دقائق • UiInteractor first-wins • إعادة تسليح retries • إلغاء مؤقت إعادة التشغيل • حراسة isFinishing.

## الكود الميت والاعتمادات
- حذف: TestBuilder/TestImports، سلسلة LabActivity (860 سطر مكرر)، ApiKeysAdapter، CandyColorHelper/AppLogger/AlphaSliderView، ~12 دالة ميتة، 15 layout، 2 lottie (70KB)، أصول xposed_legacy الأربعة، meta-data الستة القديمة، مصفوفة النطاق المكررة.
- حذف 4 اعتمادات Google ميتة 100% + Guava (8.4MB مقابل ImmutableMap واحد — CVE range) • material stable • androidx bumps • debuggable=true للـ debug • jvmTarget 17.
- مؤجل موثق: حذف 168 نص ترجمة ميت، dexkit bump، R8، DiffUtil لقائمة التطبيقات.
---

# عاشراً: توافق 16KB + إنجاز المؤجل (ISSUE-023)

- **تحذير 16KB Page Size** (Android 15+): المكتبات الخمس غير المحاذاة كلها prebuilts طرف ثالث (youtubedl-android: libpython/ffmpeg/ffprobe/qjs — DexKit: libdexkit). الإصلاح الجذري upstream بعلم `-Wl,-z,max-page-size=16384`. تحذير استشاري فقط على Android 15. • DexKit رُفع لـ 2.2.0 (محاذاة/مسح أحدث — اختبر هوك يوتيوب). • تنبيه debuggable متوقع على builds التطوير.
- **إنجاز المؤجل**: حذف 190 نص default ميت + 2136 مفتاح شبحي من 20 ترجمة (2326 إجمالاً) — resources.arsc كانت 2.2MB.
- **مؤجل للاختبار المشترك**: تفعيل R8 (proguard rules موجودة وجاهزة)، InstalledAppsAdapter → DiffUtil.
## 🔁 ISSUE-024 — انحدار "الفيديو القديم" (الجذر الحقيقي لم يكن الكاش)
- **التشخيص**: 6 مواضع في `YouTubeHook` تمرر `currentVideoId` العام للعملية إلى `openExternalDownloader`. عند فشل رصد الفيديو الجديد يُستخدم id القديم ويُفتح رابطه — والشيت يعرض بصدق ما أعطي له.
- **الإصلاح**: بوابة Stale-Guard مركزية (id من الالتقاط العام وعمره > 90 ثانية → منع + Toast عربي إرشادي + تصفير)، وتنظيف كاش آمن إضافي عند كل فتح للبوت شيت.
- **إن تكرر الـ Toast**: بلّغني — معناه تقوية رصد Layer1 (رصد الـ id لحظة النقر من player response).
---

# حادي عشر: تبني توصيات API 102 (ISSUE-025)

- **P2 — `deoptimize()`**: مطبق على المسارات الساخنة في system_server (3 overload لـ `setPrimaryClip` + `setMetadata`) — يمنع ART inline الذي يجعل الهوك صامتاً على بعض OEMs.
- **P1 (جزئي) — Remote Preferences**: نبضة التفعيل تُكتب/تُقرأ عبر `getRemotePreferences` (إدارة الإطار، بلا ملفات world-readable) كإثبات نمط؛ الترحيل الكامل للإعدادات مؤجل حتى service:102/SDK37.
- **P6 — توحيد المستمعين**: حُذف مستمع HomeFragment الساكن المنافس (كان رابع مستمع)؛ `LSPosedHelper` أصبح المصدر الوحيد مع `addServiceBindListener` للتحديث الفوري، و`onServiceDied` يعيد تسليح retries.
- **المكتبات**: core-ktx 1.16 (1.19 يتطلب AGP 9.1) • work 2.11.2 • appcompat 1.7.1 • lottie 6.7.1 • dexkit 2.2.0 • mockito 5.12 • test-ext 1.2.1 • espresso 3.6.1.
