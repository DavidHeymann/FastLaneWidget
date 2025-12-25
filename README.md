# 🚗 Fast Lane Price Widget

<div align="center">

![Version](https://img.shields.io/badge/version-3.3.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![License](https://img.shields.io/badge/license-MIT-orange.svg)

**Widget אנדרואיד חכם להצגת מחירי Fast Lane (מסלול מהיר) בישראל**

[תכונות](#-תכונות) • [התקנה](#-התקנה) • [שימוש](#-שימוש) • [בניה](#-בניה-מקומית) • [GitHub Actions](#-github-actions) • [תיעוד](#-תיעוד)

</div>

---

## 📋 תוכן עניינים

- [תיאור](#-תיאור)
- [תכונות](#-תכונות)
- [התקנה](#-התקנה)
- [שימוש](#-שימוש)
- [בניה מקומית](#-בניה-מקומית)
- [GitHub Actions](#-github-actions---בניה-אוטומטית)
- [מבנה הפרויקט](#-מבנה-הפרויקט)
- [תיעוד נוסף](#-תיעוד-נוסף)

---

## 📖 תיאור

**Fast Lane Price Widget** הוא אפליקציית Android שמציגה את מחיר המעבר הנוכחי במסלול המהיר (Fast Lane) בכבישי אגרה בישראל.

### שני סוגי Widgets:

1. **Home Widget** - widget קלאסי למסך הבית
2. **Floating Widget** - widget צף שמרחף מעל אפליקציות אחרות

---

## ✨ תכונות

### Home Widget (רגיל)

#### 🎨 עיצוב וצבעים
- ✅ **5 פלטות צבעים**: Pastel, Vibrant, Dark, Minimal, Neon
- ✅ **צבע דינמי לפי מחיר**: 
  - 🟢 ירוק - מחיר נמוך
  - 🟡 צהוב - מחיר בינוני
  - 🔴 אדום - מחיר גבוה
- ✅ **2 גדלים**: רגיל (4x1) וקטן (2x1)

#### 🔄 עדכונים
- ✅ רענון בלחיצה
- ✅ עדכון אוטומטי כשהמסך נדלק
- ✅ מחוון טעינה

#### 🔔 התראות
- ✅ התראה כשהמחיר חוצה סף
- ✅ 2 סוגי התראות (נמוך→בינוני, בינוני→גבוה)

---

### Floating Widget (צף)

#### 🎯 תכונות בסיס
- ✅ **3 גדלים**: Small, Medium, Large
- ✅ **שקיפות מתכווננת**: 10%-100%
- ✅ **גרירה חופשית** - מיקום בכל מקום
- ✅ **שמירת מיקום** - זוכר מיקום אחרון
- ✅ **רקע מעוגל לבן**

#### 🎨 צבעים
- ✅ **צבע טקסט דינמי** - משתנה לפי מחיר
- ✅ **5 פלטות זהות** ל-Home Widget
- ✅ מחוון טעינה

#### 🖱️ אינטראקציות
- ✅ **Click** → רענון מחיר
- ✅ **Drag מהיר** → הזזת מיקום
- ✅ **Long Press** (500ms) → drag-to-close:
  1. החזק 500ms
  2. עיגול אדום עם X מופיע 🎯
  3. גרור לעיגול
  4. שחרר → Widget נסגר ✅

#### 🔄 התנהגות מערכת
- ✅ חוזר אחרי מסך כבוי
- ✅ רענון אוטומטי כשהמסך נדלק

---

### ⚙️ הגדרות

#### 🎨 פלטות צבעים (משותפות)
- **Pastel** 🌸 - גוונים בהירים
- **Vibrant** 💥 - צבעים עזים
- **Dark** 🌑 - גוונים כהים
- **Minimal** ⚪ - מינימליסטי
- **Neon** 🌈 - צבעי נאון

#### 📊 סף מחירים
- **סף נמוך→בינוני**: ברירת מחדל ₪10
- **סף בינוני→גבוה**: ברירת מחדל ₪20
- **טווח**: ₪5-50

#### 🔧 Floating Widget
- גודל: Small / Medium / Large
- שקיפות: 10% - 100%
- הפעלה/כיבוי

---

## 📥 התקנה

### דרישות
- **Android**: 7.0 (API 24) ומעלה
- **הרשאות**: INTERNET, SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS

### התקנה מ-GitHub Releases

1. עבור ל-[Releases](https://github.com/YOUR_USERNAME/FastLaneWidget/releases)
2. הורד `FastLaneWidget-v3.3.0.apk`
3. התקן על המכשיר
4. אשר הרשאות

---

## 🎮 שימוש

### הוספת Home Widget
1. לחץ ארוך על מסך הבית
2. בחר **Widgets**
3. מצא **Fast Lane Price**
4. גרור למסך

### הפעלת Floating Widget
1. פתח את **Fast Lane Price**
2. לחץ **הפעל Widget צף**
3. אשר הרשאות
4. Widget יופיע

### שינוי פלטת צבעים
1. פתח אפליקציה
2. בחר **Color Theme**
3. בחר פלטה
4. לחץ **שמור**

---

## 🛠️ בניה מקומית

### דרישות
- Android Studio: Arctic Fox+
- JDK: 11+
- Gradle: 7.0+

### שלבים

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/FastLaneWidget.git
cd FastLaneWidget

# 2. Open in Android Studio
# File > Open > FastLaneWidget

# 3. Build
./gradlew assembleRelease

# APK: app/build/outputs/apk/release/
```

---

## 🤖 GitHub Actions - בניה אוטומטית

### סקירה

הפרויקט משתמש ב-GitHub Actions לבניה אוטומטית של APK.

### Workflow: `.github/workflows/build-apk.yml`

- ✅ רץ בכל push ל-main
- ✅ רץ בכל pull request
- ✅ רץ ביצירת release
- ✅ מעלה APK ל-Artifacts

### יצירת Release

#### דרך 1: GitHub UI

1. עבור ל-**Releases**
2. לחץ **Draft a new release**
3. מלא:
   ```
   Tag: v3.3.0
   Title: Fast Lane Widget v3.3.0
   Description: תיאור השינויים
   ```
4. **Publish release**

#### דרך 2: Git Tags

```bash
# צור tag
git tag -a v3.3.0 -m "Release v3.3.0"

# דחוף
git push origin v3.3.0

# עבור ל-GitHub ויצור release
```

#### דרך 3: GitHub CLI

```bash
gh release create v3.3.0 \
  --title "Fast Lane Widget v3.3.0" \
  --notes "## New Features
- Long press drag-to-close
- Screen on receiver" \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

### Release אוטומטי מלא

צור `.github/workflows/release.yml`:

```yaml
name: Create Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
          
      - name: Build APK
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease
          
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/app-release-unsigned.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**שימוש:**
```bash
git tag v3.3.0
git push origin v3.3.0
# Release נוצר אוטומטית עם APK!
```

### הורדת APK מ-Actions

1. **Actions** tab
2. בחר workflow run
3. גלול ל-**Artifacts**
4. הורד `FastLaneWidget-APK.zip`

---

## 📂 מבנה הפרויקט

```
FastLaneWidget/
├── .github/workflows/
│   └── build-apk.yml          # GitHub Actions
├── app/src/main/
│   ├── java/com/fastlane/pricewidget/
│   │   ├── FastLaneWidget.kt         # Home Widget
│   │   ├── FloatingWidgetService.kt  # Floating Widget
│   │   ├── ScreenOnReceiver.kt       # Screen On
│   │   ├── MainActivity.kt           # Settings
│   │   ├── PriceApi.kt              # API
│   │   └── WidgetPreferences.kt     # Preferences
│   └── res/
│       ├── layout/                   # UI layouts
│       ├── drawable/                 # Icons & backgrounds
│       └── values/                   # Strings, colors
├── README.md                         # ⭐ קובץ זה
└── docs/                            # תיעוד נוסף
```

---

## 📚 תיעוד נוסף

| מסמך | תיאור |
|------|-------|
| [V3.3.0_COMPLETE.md](V3.3.0_COMPLETE.md) | תיעוד גרסה 3.3.0 |
| [DRAG_TO_CLOSE_FIX.md](DRAG_TO_CLOSE_FIX.md) | מחקר drag-to-close |
| [SHARED_COLOR_PALETTE.md](SHARED_COLOR_PALETTE.md) | פלטות צבעים |

### גרסאות

- **v3.3.0** (2025-12-25) - Long press drag-to-close + Screen receiver
- **v3.2.0** (2025-12-24) - פלטות צבעים משותפות
- **v3.1.0** (2025-12-23) - Floating widget
- **v3.0.0** (2025-12-22) - Home widget + התראות

---

## 🤝 תרומה

1. Fork הפרויקט
2. צור branch (`git checkout -b feature/Feature`)
3. Commit (`git commit -m 'Add Feature'`)
4. Push (`git push origin feature/Feature`)
5. פתח Pull Request

---

## 📄 רישיון

MIT License - ראה [LICENSE](LICENSE) לפרטים

---

<div align="center">

**עשה בעברית עם ❤️**

[⬆ חזרה למעלה](#-fast-lane-price-widget)

</div>
