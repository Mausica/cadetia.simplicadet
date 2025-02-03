
# Simplify [1.1.0](https://github.com/SuperMarius409/Simplifie)

<img align="right" height="700" src="https://github.com/Simplify-Nature/simplify.app/blob/main/assets/phone.png"/>

Does school really expect you to remember when and where every class is, AND what’s due at what time?
Staying organized is harder now than ever, so even if you were a super computer you’d still struggle.
Don’t worry, we’ve got an app that will help even the laziest students stay organized, productive, and, dare we say, ON TIME!

Our `all-in-one organizational app` is designed to cater to the diverse needs of students, offering a range of functionalities to keep you on track and productive throughout your academic journey.

First and foremost, our `Note Taking App` provides a seamless and intuitive interface for capturing and organizing your notes, lectures, videos, and other important resources. You can easily create, edit, and categorize your notes into customized notebooks or folders, ensuring that all your academic materials are neatly arranged and easily accessible.

But the note-taking feature is just the tip of the iceberg. Our app goes beyond that to offer a powerful `AI Assistant` named BEN, leveraging the capabilities of artificial intelligence to provide personalized support and guidance. BEN can help you with a variety of tasks, such as setting reminders for upcoming assignments and exams, suggesting study resources based on your subjects, and even offering study tips and techniques tailored to your learning style.

Furthermore, our app includes a comprehensive task management system. You can create to-do lists, set deadlines, and receive notifications to stay on top of your assignments, projects, and extracurricular activities. The app's intuitive interface allows you to prioritize tasks, track progress, and ensure you never miss a deadline again.

To make studying more engaging and interactive, our app features a `built-in quiz game`. You can create custom quizzes based on your course materials, enabling you to reinforce your learning and assess your knowledge in a fun and gamified manner. Challenge yourself, compete with classmates, or simply use it as a self-assessment tool to gauge your understanding of the subject matter.

With our app, you'll have a comprehensive suite of tools at your disposal, enabling you to stay organized, manage your tasks effectively, access personalized support, and make learning an enjoyable and engaging experience.

So, what do you think? Are you ready to install our app and revolutionize the way you stay organized and productive as a student?

[![Repository size](https://img.shields.io/github/repo-size/kivymd/kivymd.svg)](https://github.com/SuperMarius409)

## Installation

> [!Note]
> The app has a newer version but is still on testing. 

```bash
git clone https://github.com/Simplify-Nature/simplify.app.git
```
Or access the file `releases\simplify.apk` and download the file on your phone.

## Documentation & Credits

- See documentation at [java](https://docs.oracle.com/en/java/)
- Wiki with examples of using Google Compose Widgets: [examples](https://developer.android.com/samples)
- All the icons are taken from : https://icons8.com/
- Some designs of the app are taken from [Dribble](https://dribbble.com/) or [Figma](https://www.figma.com/)
- We are using [PlatformTools](https://developer.android.com/tools/releases/platform-tools) for debugging the app
- The final app have `.apk` file extension
- The sites that we used for gething information is [GeminiAPI](https://ai.google.dev/gemini-api/docs/api-key)

[![My Skills](https://skillicons.dev/icons?i=java,kotlin,firebase,gcp,androidstudio,ae,ps,ai)](https://skillicons.dev)

## Watch Our App Demo

> [!Important]
> [![Repository icon](https://github.com/SuperMarius409/simplify.app/blob/main/assets/logo.png)](https://github.com/SuperMarius409)

<p align="left">
  <a href="https://drive.google.com/drive/folders/1A7GXL60SCmtimJzcJ3N-p_kYNe0b0Z-I?usp=sharing">
    <img 
        width="600" 
        src="https://preview.redd.it/okay-so-apparently-theres-an-issue-with-loading-or-watching-v0-rsegol869zfb1.jpg?width=640&crop=smart&auto=webp&s=415b227aa5b73effa2dda1fe8a5fa603fa5667d1" 
        title="Click to watch demo application of our app"
    >
  </a>
</p>

## Web-Powered App Integration

```kotlin
val generativeModel = GenerativeModel(
    // The Gemini 1.5 models are versatile and work with both text-only and multimodal prompts
    modelName = "gemini-1.5-flash-latest",
    // Access your API key as a Build Configuration variable (see "Set up your API key" above)
    apiKey = BuildConfig.apiKey
)

val prompt = "Write a story about a magic backpack."
val response = generativeModel.generateContent(prompt)
print(response.text)
```

This essential function serves as the **backbone** for all our apps. It utilizes an `API` to retrieve `JSON` data and dynamically updates the app's text based on the received information. In this example we use [GeminiAPI](https://ai.google.dev/gemini-api/docs/api-key) to get the json data and update it to our app. We rely on this function across our apps to seamlessly integrate real-time data and provide users with **up-to-date** and relevant content.

