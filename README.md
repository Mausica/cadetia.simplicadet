# Simplicadet `v1.4b` 🎖️📚

<div align="center">
  <img src="https://github.com/Mausica/cadetia.simplicadet/blob/main/assets/phone.png" alt="Simplicadet Preview" height="600"/>
</div>

## 📋 Despre Proiect

**Simplicadet** este o aplicație mobilă Android de ultimă generație, dezvoltată cu precizie militară pentru cadeții din instituțiile de învățământ militar românești și nu numai. Aplicația oferă o soluție completă și adaptabilă pentru gestionarea sarcinilor, pregătirea pentru examene și menținerea disciplinei prin instrumente digitale moderne.

> *"Disciplina este sufletul unei armate."* – George Washington

### 🎯 Obiectivele Aplicației

- **Modernizarea educației militare** prin implementarea tehnologiilor digitale
- **Optimizarea procesului de învățare** pentru cadeți și studenți civili
- **Standardizarea metodelor de evaluare** și urmărire a progresului
- **Facilitarea comunicării** între instituții și studenți
- **Îmbunătățirea disciplinei** prin sisteme automatizate de monitorizare

## ✨ Funcționalități Principale

### 📝 Gestionare Elegantă a Sarcinilor
- **Organizare Precisă:** Programarea temelor, exercițiilor militare și termenelor academice
- **Notificări Inteligente:** Sistem avansat de notificări pentru a nu rata activități critice
- **Prioritizare Strategică:** Adaptarea programului pe baza urgenței și importanței

### 📡 Flux de Știri în Timp Real
- **Actualizări CNMTV:** Informații în timp real de la instituțiile partenere
- **Buletine Instantanee:** Anunțuri pentru exerciții, evenimente și sesiuni de pregătire
- **Notificări Push:** Alerturi automate pentru evenimente importante

### 🏆 Quizuri Dinamice & Clasamente
- **Quizuri Reglementare Interactive:** Testarea cunoștințelor despre regulamentele militare
- **Firebase Cloud Integration:** Sincronizare în timp real cu standardele actuale
- **Sistem de Punctaj:** Câștigarea de puncte pentru fiecare răspuns corect
- **Clasamente Competitive:** Vizualizarea progresului față de colegii din instituție

### 🗺️ Hartă de Formare & Monitorizare Prezență
- **Vizualizare Digitală:** Reprezentarea dinamică a formației unității
- **Gestionare Apel:** Identificarea rapidă a absenților și urmărirea prezenței
- **Responsabilitate Sporită:** Menținerea ordinii prin sisteme vizuale clare

### 📚 Sistem de Învățare Adaptiv
- **Căi de Învățare Personalizate:** Parcursuri educaționale structurate pe niveluri
- **Flashcard-uri Inteligente:** Sistem de memorare prin repetiție spațiată
- **Conținut Actualizat:** Materiale didactice sincronizate cu curriculumul oficial

### 🔐 Gestionare Avansată a Utilizatorilor
- **Coduri de Acces:** Sistem securizat pentru înregistrarea în instituții
- **Roluri Diferențiate:** Permisiuni separate pentru studenți, instructori și administratori
- **Profil Personalizat:** Informații complete despre rank, pluton și progres academic

## 🛠️ Arhitectura Tehnică

### Tehnologii Utilizate
- **Limbaje de Programare:** Java, Kotlin
- **Framework UI:** Android Native + XML
- **Backend:** Firebase Firestore, Firebase Authentication
- **Cloud Services:** Google Cloud Platform
- **IDE:** Android Studio
- **Instrumente Design:** Adobe After Effects, Photoshop, Illustrator

### Structura Aplicației
```
com.cadetia.simplicadet/
├── activities/          # Activități principale
├── ui/                  # Componente de interfață
├── database/           # Gestionare date și Firebase
├── entities/           # Modele de date
├── utils/              # Utilitare și helpers
└── listeners/          # Event handlers
```

### Paradigme de Programare
- **Model-View-ViewModel (MVVM)** pentru separarea logicii
- **Repository Pattern** pentru gestionarea datelor
- **Observer Pattern** pentru actualizări în timp real
- **Singleton Pattern** pentru Firebase connections

## 📱 Cerințe de Sistem

### Cerințe Minime
- **Sistem de Operare:** Android 8.0 (API level 26) sau mai nou
- **RAM:** 3 GB minim, 4 GB recomandat
- **Spațiu de Stocare:** 150 MB liberi pentru instalare
- **Conexiune Internet:** WiFi sau Date Mobile pentru funcționalitate completă
- **Permisiuni:** Acces la stocare, cameră foto și internet

### Cerințe Recomandate
- **Sistem de Operare:** Android 10.0 (API level 29) sau mai nou
- **RAM:** 6 GB sau mai mult
- **Procesor:** Snapdragon 660 / Exynos 9611 sau superior
- **Rezoluție:** 1080p (Full HD) sau mai bună

### Compatibilitate
- **Orientare:** Aplicația funcționează exclusiv în modul portrait
- **Limbi Suportate:** Română (RO), Engleză (EN-GB), Franceză (FR), Spaniolă (ES)
- **Instituții Suportate:** CNMTV și alte instituții militare românești

## 🚀 Instalare și Configurare

### Instalare Rapidă
```bash
# Clonarea repository-ului
git clone https://github.com/Mausica/simplicadet.git

# Deschiderea în Android Studio
# File -> Open -> Selectați folderul simplicadet
```

### Download Direct APK
[<kbd> <br> 📥 Descarcă Simplicadet v1.4b <br> </kbd>](https://github.com/Mausica/cadetia.simplicadet/raw/refs/heads/main/releases/simplicadet.apk)

### Configurarea Proiectului
1. **Android Studio:** Versiunea 2024.1.1 sau mai nouă
2. **Gradle:** Configurare automată prin `build.gradle.kts`
3. **Firebase:** Conectarea la serviciile cloud prin `google-services.json`
4. **Dependințe:** Instalare automată la primul build

## 🔧 Funcționalități Avansate

### Pentru Administratori
- **Upload Masiv:** Încărcarea în lot a studenților prin fișiere Excel
- **Gestionare Quizuri:** Adăugarea și modificarea întrebărilor din aplicație
- **Statistici Detaliate:** Monitorizarea progresului la nivel institutional
- **Coduri de Acces:** Generarea și distribuirea codurilor pentru studenți noi

### Pentru Studenți
- **Profil Complet:** Informații despre rank, pluton, înălțime și progres
- **Progres Visual:** Grafice interactive pentru urmărirea performanțelor
- **Notificări Personalizate:** Alerte adaptate programului individual
- **Competiții:** Participarea la concursuri între instituții

### Securitate și Confidențialitate
- **Autentificare Multiplă:** Google, Facebook și email tradițional
- **Criptare Date:** Toate informațiile sunt criptate în tranzit și la rest
- **Backup Automatic:** Sincronizare cloud pentru siguranța datelor
- **GDPR Compliant:** Respectarea reglementărilor europene de protecție a datelor

## 📊 Statistici Proiect

### Metrici de Dezvoltare
- **Linii de Cod:** ~25,000 (Java/Kotlin)
- **Fișiere Surse:** 120+ fișiere
- **Dependințe:** 35+ librării externe
- **Versiuni Release:** 4 versiuni stabile

### Performance
- **Timp de Pornire:** <3 secunde pe dispozitive mid-range
- **Consum RAM:** 180-250 MB în timpul rulării
- **Dimensiune APK:** ~45 MB (versiunea finală)
- **Offline Support:** Funcționalitate parțială fără internet

## 🎨 Design și UX/UI

### Principii de Design
- **Dark/Light Theme:** Suport automat pentru temele sistemului
- **Animații Fluide:** Tranziții optimizate pentru experiență premium
- **Accessibility:** Suport pentru utilizatori cu dizabilități

### Paleta de Culori
- **Primary:** #1976D2 (Albastru institutional)
- **Secondary:** #388E3C (Verde militar)
- **Background:** Adaptat automat la tema sistemului

## 🔗 API și Integrări

### Firebase Services
- **Firestore Database:** Baza de date NoSQL scalabilă
- **Firebase Auth:** Autentificare sigură multi-platform
- **Cloud Functions:** Logică server-side pentru operațiuni complexe
- **Firebase Analytics:** Monitorizarea utilizării și crash-urilor

### API-uri Externe
- **Gemini AI:** Integrare pentru conținut generat de AI
- **Google Ads:** Monetizare prin reclame relevante
- **Google Play Services:** Funcționalități native Android

## 📚 Documentație și Resurse

### Documentație Tehnică
- **JavaDoc:** [Documentație Java](https://docs.oracle.com/en/java/)
- **Android Samples:** [Exemple Google](https://developer.android.com/samples)
- **Firebase Docs:** [Ghiduri Firebase](https://firebase.google.com/docs)

### Resurse Design
- **Icoane:** [Icons8](https://icons8.com/) - Set complet de icoane profesionale
- **Design Templates:** [Dribbble](https://dribbble.com/) și [Figma](https://www.figma.com/)
- **Fonts:** Circular și Ubuntu pentru consistență

### Instrumente de Dezvoltare
- **Android Studio:** IDE principal pentru dezvoltare
- **Platform Tools:** [Android SDK Tools](https://developer.android.com/tools/releases/platform-tools)
- **Git:** Versioning și colaborare prin GitHub

## 🤝 Contribuții și Suport

### Pentru Dezvoltatori
```bash
# Fork repository-ul
# Creați o branches nouă pentru feature-ul vostru
git checkout -b feature/nume-feature

# Commit changes
git commit -m "Adaugă feature nou"

# Push la branch
git push origin feature/nume-feature

# Creați Pull Request
```

### Raportarea Bug-urilor
1. **Issues GitHub:** Descriere detaliată a problemei
2. **Logs:** Includeți logurile aplicației dacă este posibil
3. **Device Info:** Model telefon, versiune Android, versiune aplicație
4. **Steps to Reproduce:** Pași clari pentru reproducerea bug-ului

### Contact și Suport
- **Email:** mausica.contact@gmail.com
- **GitHub Issues:** Pentru probleme tehnice
- **Discord:** Server comunitate pentru discuții
- **Wiki:** Ghiduri detaliate de utilizare

## 📄 Licență și Drepturi

### Licența MIT
Acest proiect este licențiat sub MIT License - vezi fișierul [LICENSE](LICENSE) pentru detalii.

### Drepturi de Autor
- **Cod Sursă:** © 2025 Echipa Simplicadet
- **Design Assets:** Toate drepturile rezervate
- **Trademark:** "Simplicadet" este marcă înregistrată

### Contribuitori
```
👨‍💻 Lead Developer: Mausica
🎨 UI/UX Designer: Echipa Design
🔧 Backend Engineer: Firebase Team
📚 Content Creator: Instructori CNMTV
```

## 🚀 Roadmap și Viitor

### Versiunea 1.5 (Q2 2025)
- [ ] Integrare AR pentru simulări militare
- [ ] Chatbot AI pentru asistență 24/7
- [ ] Sistem de badge-uri și achievements
- [ ] Export rapoarte în format PDF

### Versiunea 2.0 (Q4 2026)
- [ ] Versiune Web complementară
- [ ] Integrare cu alte instituții europene
- [ ] Sistem de mentoring între studenți
- [ ] Machine Learning pentru predicții progres

---

<div align="center">

### 🎖️ *"În precizie găsim excelența, în disciplină găsim forța."*

**Simplicadet v1.4b** - *Educația militară redefinită pentru era digitală*

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://java.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com/)

[📱 Download](https://github.com/Mausica/cadetia.simplicadet/releases) • [📚 Wiki](https://github.com/Mausica/simplicadet/wiki) • [🐛 Issues](https://github.com/Mausica/simplicadet/issues) • [💬 Discussions](https://github.com/Mausica/simplicadet/discussions)

</div>
