# المعمارية المقترحة

## قرارات أساسية
Kotlin وJetpack Compose وMaterial 3، مع Clean Architecture وMVVM. التطبيق لا يحتاج Backend أو شبكة تشغيلية. DataStore للإعدادات، Room لسجل البيانات الوصفية، وSAF/ContentResolver للملفات. كل محرك PDF/OCR/مسح خلف واجهة مستقلة قابلة للتبديل.

## الطبقات
- **Presentation:** Compose، Navigation، ViewModels، حالات UI sealed عبر StateFlow. لا تتعامل الشاشة مع تفاصيل المحركات.
- **Domain:** كيانات وقواعد استخدام مستقلة قدر الإمكان عن Android. أمثلة: MergePdfs، SplitPdf، RenderPdfPages، OptimizePdf، CompressPdf، ScanDocument، RecognizeText، FillPdfForm.
- **Data:** المستودعات وDAO وDataStore وSAF adapters وموائمات المحركات وإدارة الملفات المؤقتة.

## تقسيم Gradle مبدئي
- `:app`: نقطة الدخول، الحقن، التنقل.
- `:core:common`: النتائج والأخطاء والأنواع المشتركة.
- `:core:ui`: الثيم والمكونات والتوطين.
- `:core:files`: SAF والـURI والحفظ والتدفقات.
- `:domain`: العقود وحالات الاستخدام.
- `:data`: Room/DataStore والمستودعات.
- `:feature:home`, `:feature:history`, `:feature:settings`, `:feature:scanner`, `:feature:pdf`, `:feature:ocr`.

ابدأ قليل الموديولات؛ لا تنشئ موديولًا لكل زر قبل ثبات الحدود.

## تدفق الملف
1. المستخدم يمنح وصولًا لملف عبر SAF.
2. يسجل التطبيق URI والاسم والنوع والحجم المتاح، ويطلب صلاحية طويلة الأمد عند دعم المزود.
3. UseCase يتحقق من الملف وينشئ مؤقتًا في cache عند ضرورة المحرك لمسار تقليدي.
4. المعالجة خارج Main thread مع تقدم وإلغاء.
5. يتحقق التطبيق من صلاحية الناتج وحجمه.
6. يختار المستخدم وجهة عبر منتقي الحفظ.
7. ينسخ الناتج للوجهة ويسجل URI العملية.
8. تزال الملفات المؤقتة عند النجاح أو الفشل أو الإلغاء.

لا تفترض أن URI مسار ملف عادي. قد يصبح الملف غير متاح بعد النقل أو الحذف.

## نماذج السجل
**DocumentRecord:** id، displayName، mimeType، uriString، sizeBytes اختياري، createdAt، availability، sourceOperationId اختياري.

**OperationRecord:** id، toolId، status، startedAt، completedAt اختياري، inputDocumentIds، outputDocumentId اختياري، settingsSummary غير حساس، errorCode اختياري.

لا تحفظ محتوى الملفات أو النص المستخرج تلقائيًا.

## الخصوصية والأمان
- لا حسابات أو أسرار شبكة.
- إزالة INTERNET من Manifest الإنتاجي والتحقق من Manifest النهائي والتبعيات.
- BiometricPrompt وبيانات اعتماد الجهاز للقفل؛ القفل لا يعني تشفير ملفات المستخدم.
- لا محتوى PDF أو OCR في Logcat.
- تنظيف cache بعد الإتمام وبعد إعادة التشغيل عقب توقف مفاجئ.
- استخدام SAF بدل صلاحيات التخزين الشاملة.
- تضمين نماذج OCR المطلوبة؛ لا تنزيل وقت التشغيل.
- فحص تراخيص المحركات والخطوط وبيانات اللغة.

## المهام الطويلة
عرض تقدم وإلغاء في الضغط والتصدير وOCR. استخدم coroutines للعمليات داخل الجلسة، وWorkManager فقط إذا وجب استمرارها بعد مغادرة التطبيق/إعادة تشغيل النظام. الإلغاء لا يمس الملف الأصلي.

## واجهات المحركات
- PdfDocumentEngine: inspect, merge, split, render, rotate, crop, watermark.
- PdfOptimizationEngine: optimize, compress.
- PdfFormEngine: inspectFields, fillFields.
- PdfRepairEngine: repair مع تقرير استرداد.
- OcrEngine: recognize image/page مع لغات محددة.
- ScannerEngine: detect edges, perspective correction, enhancement.

كل محرك يعلن Capabilities حتى لا تعرض الواجهة زرًا غير مدعوم. لا توحّد عمليتي Optimize وCompress إن كانت المكتبة تنفذهما بالطريقة نفسها.

## الماسح وOCR
التقاط عبر CameraX أو منتقي الصور، والمعالجة محلية. لا تعتمد على Google Play Services Document Scanner لضمان الأوفلاين. OCR خلف OcrEngine مع ملفات لغة مضمّنة. PDF القابل للبحث يحتاج طبقة نص موضوعة فوق الصور، واستخراج TXT وحده لا يحقق ذلك.

## RTL والتوطين
موارد strings منفصلة للعربية والإنجليزية. اختبر RTL فعليًا بدل عكس العناصر تلقائيًا. اللغة تضبط اتجاه الشاشة، مع توطين التواريخ والأرقام والتحميل والفراغ والخطأ. الصفحة الرئيسية تصنف الأدوات إلى مسح، تنظيم، تحويل، تحسين، تحرير، OCR.

## الجودة والتسليم
GitHub Actions للبناء وlint واختبارات الوحدة وفحص الرخص وManifest. R8 للإصدار وإزالة السجلات الحساسة. التعتيم لا يغني عن حماية البيانات. اختبر URI وPDF عربي وملف تالف ونفاد المساحة. لا تدّع نجاح تحقق لم يُشغّل.

