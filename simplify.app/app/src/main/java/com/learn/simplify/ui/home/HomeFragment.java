package com.learn.simplify.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.learn.simplify.R;
import com.learn.simplify.activities.Home;
import com.learn.simplify.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment{

    private FragmentHomeBinding binding;
    private Button selectedButton;
    ShapeableImageView mainProfileButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentHomeBinding binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        replaceFragment(new HomeFragment1());
        setupButtons(root);
        mainProfileButton = root.findViewById(R.id.mainProfileButton);
        mainProfileButton.setOnClickListener(v -> {
            Home homeActivity = (Home) requireActivity();
            homeActivity.openNavigationDrawer();
        });

        retrieveUserData();
        return root;
    }


    private void setupButtons(View view) {
        // Buttons for your options
        Button allButton = view.findViewById(R.id.home_ac1);
        Button notesButton = view.findViewById(R.id.home_ac2);
        Button flashcardsButton = view.findViewById(R.id.home_ac3);
        Button tasksButton = view.findViewById(R.id.home_ac4);

        // Set up the Button listeners
        setupButton(allButton, R.id.action_navigation_home_to_homeFragment1, new HomeFragment1());
        setupButton(notesButton, R.id.action_navigation_home_to_homeFragment2, new HomeFragment2());
        setupButton(flashcardsButton, R.id.action_navigation_home_to_homeFragment3, new HomeFragment3());
        setupButton(tasksButton, R.id.action_navigation_home_to_homeFragment4, new HomeFragment4());

        // Set the allButton as the default selected button
        selectButton(allButton);
    }



    private void setupButton(Button button, int destinationId, Fragment fragment) {
        button.setOnClickListener(view -> {
            // Check if the clicked button is already selected
            if (view.isSelected()) {
                // Refresh data when an activated button is clicked again
                //loadNotesInBackground();
                return;
            }

            // Deselect the currently selected button (if any)
            if (selectedButton != null) {
                selectedButton.setSelected(false);
                updateButtonState(selectedButton, false);
            }

            // Select the clicked button
            selectedButton = button;
            selectedButton.setSelected(true);
            updateButtonState(selectedButton, true);

            // Replace the fragment in the container
            replaceFragment(fragment);
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.smallerFragmentContainer, fragment);
        transaction.addToBackStack(null);  // Optional: Add the transaction to the back stack
        transaction.commit();
    }

    private void selectButton(Button button) {
        // Select the button programmatically
        selectedButton = button;
        selectedButton.setSelected(true);
        updateButtonState(selectedButton, true);
    }

    private void updateButtonState(Button button, boolean isChecked) {
        if (isChecked) {
            button.setBackgroundResource(R.drawable.button_green);
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        } else {
            button.setBackgroundResource(R.drawable.button_nothing);
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void retrieveUserData() {
        // Get the SharedPreferences object
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);

        // Retrieve the values using the keys
        String userPhoto = sharedPreferences.getString("userPhoto", "");

        if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")){
            Glide.with(this).load(R.raw.guest).into(mainProfileButton);
        }else {
            Glide.with(this).load(userPhoto).into(mainProfileButton);
        }


    }

}
