# 🚀 Fast Lane Widget - הוראות התקנה מלאות

## מה הכנתי לך:

✅ Widget אנדרואיד מלא  
✅ עדכון אוטומטי חכם (א'-ה', 07:00-12:00)  
✅ צבעים דינמיים לפי מחיר  
✅ GitHub Actions שבונה את ה-APK אוטומטית  

---

## 📋 שלב 1: צור חשבון GitHub (אם אין לך)

1. לך ל-https://github.com
2. לחץ "Sign up"
3. מלא פרטים ואשר מייל
4. זה חינם לגמרי!

---

## 📤 שלב 2: העלה את הקוד ל-GitHub

### אופציה א: דרך האתר (הכי פשוט!)

1. **צור repository חדש:**
   - לחץ על ה-`+` בפינה הימנית העליונה
   - בחר "New repository"
   - שם: `FastLaneWidget`
   - סמן: ✅ Public
   - לחץ "Create repository"

2. **העלה קבצים:**
   - לחץ "uploading an existing file"
   - גרור את **כל הקבצים** מהתיקייה `FastLaneWidget` (לא את התיקייה עצמה!)
   - לחץ "Commit changes"

### אופציה ב: דרך Git (אם יש לך)

```bash
cd FastLaneWidget
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/FastLaneWidget.git
git push -u origin main
```

---

## 🔨 שלב 3: GitHub Actions יבנה את ה-APK

1. לך ל-repository שיצרת
2. לחץ על טאב **"Actions"** למעלה
3. **אם אתה רואה:**
   - "Workflows" - אתה טוב! ה-workflow מזוהה
   - הודעת אישור - לחץ "I understand, enable them"

4. **הרץ את ה-build:**
   - לחץ על "Build Android APK" בצד שמאל
   - לחץ "Run workflow" (כפתור ירוק)
   - לחץ "Run workflow" שוב
   - חכה 3-5 דקות ⏳

---

## 📥 שלב 4: הורד את ה-APK

1. כשה-build מסתיים (✅ ירוק), לחץ עליו
2. גלול למטה ל-**"Artifacts"**
3. לחץ על **"FastLaneWidget-Debug"**
4. קובץ ZIP יורד - חלץ אותו
5. בפנים יש `app-debug.apk` - זה ה-APK שלך! 🎉

---

## 📱 שלב 5: התקן את ה-Widget

1. **העבר את ה-APK לטלפון** (USB / Google Drive / Email)

2. **התקן:**
   - פתח את הקובץ APK בטלפון
   - אם מופיעה אזהרה "מקור לא מוכר":
     - הגדרות → אבטחה → אפשר "מקורות לא ידועים"
     - או אשר התקנה חד-פעמית
   - לחץ "התקן"

3. **הוסף Widget למסך הבית:**
   - לחיצה ארוכה על המסך הבית
   - בחר "Widgets"
   - חפש "Nativ Mahir Price"
   - גרור למסך הבית
   - הנה! 🎊

---

## 🎨 איך ה-Widget עובד:

### עדכונים אוטומטיים
- **א'-ה', 07:00-12:00**: מתעדכן כל 20 שניות
- **שאר הזמן**: לחץ על ה-widget לרענון ידני

### צבעי רקע
- 🟢 **ירוק**: ₪10 ומטה
- 🟡 **צהוב**: ₪11-25
- 🔴 **אדום**: ₪26+

### תצוגה
- מחיר גדול ובולט
- אייקון של כביש
- זמן עדכון אחרון

---

## 🔧 פתרון בעיות

### "Build failed" ב-GitHub Actions

**בדוק את gradle-wrapper.jar:**

אם ה-build נכשל, זה בגלל שחסר קובץ. תקן ככה:

1. **אם יש לך Git:**
```bash
cd FastLaneWidget
gradle wrapper
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add gradle wrapper jar"
git push
```

2. **אם אין לך Git:**
   - הורד: https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar
   - שמור ב: `FastLaneWidget/gradle/wrapper/gradle-wrapper.jar`
   - העלה ידנית דרך GitHub website

אחרי זה רוץ שוב את ה-workflow!

### Widget לא מתעדכן

- בדוק חיבור לאינטרנט
- וודא שההרשאות אושרו
- נסה לחיצה ידנית על ה-widget

### מחיר לא מוצג

- רענן ידנית (לחץ על widget)
- בדוק שהשרת של הנתיב המהיר תקין

---

## 🎯 עדכונים עתידיים

כל פעם שתרצה לשנות משהו:
1. ערוך את הקוד
2. העלה ל-GitHub
3. GitHub Actions יבנה APK חדש אוטומטית
4. הורד והתקן

---

## 💡 רעיונות להמשך

- הודעות push כשהמחיר יורד
- גרף מחירים
- חיזוי מחירים
- אפשרות לבחור גודל widget

---

## 📞 צריך עזרה?

יש בעיה? פתח Issue ב-GitHub או שאל אותי!

---

**נהנה? ⭐ תן כוכב ל-repository!**
