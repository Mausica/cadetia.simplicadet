package com.learn.simplify.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.learn.simplify.R;
import com.learn.simplify.adapters.ChatAdapter;
import com.learn.simplify.ads.InterstitialAdd;
import com.learn.simplify.kotlin.AIModel;
import com.learn.simplify.activities.CreateQuestionBottom;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment3 extends Fragment implements CreateQuestionBottom.OnQuestionAskedListener {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private AIModel aiModel;
    private Handler handler = new Handler();
    private InterstitialAdd interstitialAdd;
    private static final String SHARED_PREFS_KEY = "chat_messages";

    public HomeFragment3() {
        // Required empty public constructor
    }

    public static HomeFragment3 newInstance() {
        return new HomeFragment3();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home3, container, false);
        FloatingActionButton addQuestion = view.findViewById(R.id.fabAssistant);
        addQuestion.setOnClickListener(v -> showCreateQuestion());

        interstitialAdd = new InterstitialAdd(); // Initialize the interstitial ad
        interstitialAdd.loadInterstitialAd(requireActivity()); // Load the ad
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        chatAdapter = new ChatAdapter(loadChatMessages());
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup AI model
        aiModel = new ViewModelProvider(requireActivity()).get(AIModel.class);
        aiModel.getResponseLiveData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String response) {
                if (response != null && !isDuplicateMessage("BEN", response)) {
                    addMessageToChat("BEN", response);
                    handler.postDelayed(() -> {
                        if (interstitialAdd != null) {
                            interstitialAdd.showInterstitialAd(requireActivity());
                        }
                    }, 500);

                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add a default "Hello" message if there are no messages
        if (chatAdapter.getItemCount() == 0) {
            Log.d("HomeFragment3", "Start by adding a question ");
        }
    }

    private void showCreateQuestion() {
        CreateQuestionBottom createQuestionBottom = CreateQuestionBottom.newInstance();
        createQuestionBottom.show(getChildFragmentManager(), createQuestionBottom.getTag());
    }

    @Override
    public void onQuestionAsked(String sender, String prompt, String response) {
        Log.d("HomeFragment3", "Received response: " + response);
        if (!isDuplicateMessage(sender, response)) {
            addMessageToChat(sender, response);
        }
    }

    public void addMessageToChat(String sender, String message) {
        chatAdapter.addMessage(sender, message);
        requireActivity().runOnUiThread(() -> {
            int position = chatAdapter.getItemCount() - 1;
            chatAdapter.notifyItemInserted(position);
            chatRecyclerView.smoothScrollToPosition(position);

        });
        saveChatMessages(chatAdapter.getMessages());
    }

    private boolean isDuplicateMessage(String sender, String message) {
        List<ChatAdapter.ChatMessage> messages = chatAdapter.getMessages();
        for (ChatAdapter.ChatMessage msg : messages) {
            if (msg.getSender().equals(sender) && msg.getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    private void saveChatMessages(List<ChatAdapter.ChatMessage> messages) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String messagesJson = gson.toJson(messages);
        editor.putString("messages", messagesJson);
        editor.apply();
        Log.d("HomeFragment3", "Messages saved");
    }

    private List<ChatAdapter.ChatMessage> loadChatMessages() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String messagesJson = sharedPreferences.getString("messages", null);
        List<ChatAdapter.ChatMessage> loadedMessages = new ArrayList<>();
        if (messagesJson != null) {
            try {
                Type type = new TypeToken<ArrayList<ChatAdapter.ChatMessage>>() {}.getType();
                loadedMessages = gson.fromJson(messagesJson, type);
            } catch (JsonSyntaxException e) {
                Log.e("HomeFragment3", "Error parsing JSON: " + e.getMessage());
            }
        }
        Log.d("HomeFragment3", "Messages loaded");
        return loadedMessages;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearChatMessages();
        aiModel.getResponseLiveData().removeObservers(getViewLifecycleOwner());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Other cleanup code if needed
    }
    @Override
    public void onStop() {
        super.onStop();
        clearChatMessages();
        aiModel.getResponseLiveData().removeObservers(getViewLifecycleOwner());
    }

    private void clearChatMessages() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();
        chatAdapter.clearMessages();
        requireActivity().runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
        Log.d("HomeFragment3", "Messages cleared");
    }
}
