package com.learn.simplify.kotlin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.learn.simplify.ads.InterstitialAdd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIModel : ViewModel() {

    private val _responseLiveData = MutableLiveData<String?>()
    val responseLiveData: LiveData<String?> get() = _responseLiveData
    private val apiKey: String = ""

    private val model = GenerativeModel(
        "gemini-1.5-pro-latest",
        apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 512
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        ),
    )

    fun generateAIContent(prompt: String) {
        viewModelScope.launch {
            try {
                Log.e("AIModel", "Generating AI content for prompt: $prompt")
                val response = withContext(Dispatchers.IO) {
                    model.generateContent(
                        content {
                            text("Your name is BEN, you are a friendly assistant who works for Simplify. Simplify is a mobile app that helps students and people to create basic notes, tasks, and quizzes. You have the job to say exactly the answer the user wants.")
                            text("Prompt: $prompt")
                        }
                    )
                }
                val generatedText = response.text
                Log.e("AIModel", "Generated response: $generatedText")
                _responseLiveData.postValue(generatedText)

            } catch (e: Exception) {
                Log.e("AIModel", "Error generating AI content", e)
                _responseLiveData.postValue("Error generating response. Please try again.")
            }
        }
    }
}
