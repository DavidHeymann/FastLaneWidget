# Nativ Mahir Price Widget

Widget אנדרואיד שמציג את המחיר הנוכחי בנתיב המהיר.

## תכונות

- 🎨 גרדיאנט פסטל עדין לפי המחיר:
  - ירוק רך: ₪10 ומטה (#A8E6CF → #C1F0D5)
  - צהוב רך: ₪11-25 (#FFE5B4 → #FFF4D6)
  - אדום רך: ₪26+ (#FFB3BA → #FFCCD1)

- ⏰ עדכון אוטומטי חכם:
  - ימים א'-ה', 07:00-12:00: עדכון כל 20 שניות
  - שאר הזמן: עדכון ידני בלחיצה על ה-widget

- 📱 Widget מינימליסטי ונקי עם טקסט כהה לקריאות מושלמת

## איך לבנות APK

### אופציה 1: Android Studio
1. פתח את התיקייה `FastLaneWidget` ב-Android Studio
2. סנכרן Gradle files
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. ה-APK יהיה ב: `app/build/outputs/apk/debug/app-debug.apk`

### אופציה 2: Command Line (אם יש לך Android SDK)
```bash
cd FastLaneWidget
./gradlew assembleDebug
```

## איך להתקין

1. העבר את הקובץ APK לטלפון
2. פתח את הקובץ והתקן (צריך לאפשר "מקורות לא ידועים")
3. לחץ לחיצה ארוכה על המסך הבית
4. בחר Widgets
5. גרור את "Nativ Mahir Price" למסך הבית

## API

Widget משתמש ב-endpoint הרשמי:
```
POST https://fastlane.co.il/PageMethodsService.asmx/GetCurrentPrice
Content-Type: application/json
Body: {}
```

## הערות

- דורש אנדרואיד 8.0 (API 26) ומעלה
- דורש הרשאת אינטרנט
- Widget לא יעדכן כשאין חיבור לאינטרנט

## פיתוח נוסף

רעיונות לעתיד:
- הודעה כשהמחיר יורד מתחת לסכום מסוים
- גרף מחירים היסטורי
- חיזוי מחירים לפי דפוסים

---

Made with ❤️ for Israeli drivers
