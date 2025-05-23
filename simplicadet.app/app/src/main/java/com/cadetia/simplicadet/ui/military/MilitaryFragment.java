package com.cadetia.simplicadet.ui.military;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.Home;
import com.cadetia.simplicadet.activities.MainActivity;
import com.cadetia.simplicadet.databinding.FragmentMilitaryBinding;
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MilitaryFragment extends Fragment {

    private static final String TAG = "MilitaryFragment";
    private FragmentMilitaryBinding binding;
    private Button selectedButton;
    ShapeableImageView mainProfileButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMilitaryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Check user authorization before loading fragment content
        checkUserAuthorization(root);

        mainProfileButton = root.findViewById(R.id.mainProfileButton);
        mainProfileButton.setOnClickListener(v -> {
            Home homeActivity = (Home) requireActivity();
            homeActivity.openNavigationDrawer();
        });

        retrieveUserData();
        return root;
    }

    private void checkUserAuthorization(View root) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // Not logged in
            showAccessDeniedDialog();
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            showAccessDeniedDialog();
            return;
        }

        // Check if user email is in the authorized list
        firestore.collection("MILITARY")
                .document("RO")
                .collection("CNMTV")
                .document("STUDENTS")
                .collection("CP2")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User is authorized
                        Log.d(TAG, "User is authorized: " + userEmail);
                        initializeFragment();
                    } else {
                        // User is not authorized
                        Log.d(TAG, "User is not authorized: " + userEmail);
                        showAccessDeniedDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user authorization", e);
                    Toast.makeText(requireContext(), "Error checking authorization", Toast.LENGTH_SHORT).show();
                    showAccessDeniedDialog();
                });
    }

    private void showAccessDeniedDialog() {
        DialogConfirm.show(
                requireContext(),
                getString(R.string.access_denied),
                getString(R.string.login_organization_email),
                () -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    startActivity(intent);
                },
                false
        );
    }

    private void initializeFragment() {
        // Initialize the fragment content after successful authorization
        replaceFragment(new MilitaryFragment1());
        setupButtons(binding.getRoot());
    }

    private void setupButtons(View view) {
        // Buttons for your options
        Button allButton = view.findViewById(R.id.military_ac1);
        Button notesButton = view.findViewById(R.id.military_ac2);
        Button formationButton = view.findViewById(R.id.military_ac3);
        Button tasksButton = view.findViewById(R.id.military_ac4);

        // Set up the Button listeners
        setupButton(allButton, R.id.action_navigation_military_to_militaryFragment1, new MilitaryFragment1());
        setupButton(notesButton, R.id.action_navigation_military_to_militaryFragment2, new MilitaryFragment2());
        setupButton(formationButton, R.id.action_navigation_military_to_militaryFragment3, new MilitaryFragment3());
        setupButton(tasksButton, R.id.action_navigation_military_to_militaryFragment4, new MilitaryFragment4());

        // Set the allButton as the default selected button
        selectButton(allButton);
    }

    private void setupButton(Button button, int destinationId, Fragment fragment) {
        button.setOnClickListener(view -> {
            if (view.isSelected()) {
                // Refresh data when an activated button is clicked again
                return;
            }

            // Update FAB icon
            if (getActivity() instanceof Home) {
                ((Home) getActivity()).updateFabIcon(fragment);
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

            replaceFragment(fragment);
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.setCustomAnimations(
                R.anim.fade_in,  // enter animation
                R.anim.fade_out, // exit animation
                R.anim.fade_in,  // pop enter animation
                R.anim.fade_out  // pop exit animation
        );

        transaction.replace(R.id.smallerFragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();

        // Add callback after transaction completes
        fragmentManager.executePendingTransactions();
        new Handler().post(() -> {
            if (getActivity() instanceof Home) {
                ((Home) getActivity()).updateFabIcon(this);
            }
        });
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
            button.setBackgroundResource(R.drawable.button_social);
            TypedArray typedArray = requireContext().getTheme().obtainStyledAttributes(
                    new int[]{com.google.android.material.R.attr.textAppearanceBody1}
            );
            int textColor = typedArray.getColor(0, ContextCompat.getColor(requireContext(), R.color.white));
            typedArray.recycle();
            button.setTextColor(textColor);
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
            Glide.with(this).load(R.raw.guest_civil).into(mainProfileButton);
        }else {
            Glide.with(this).load(userPhoto).into(mainProfileButton);
        }
    }

    public void actionController() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.smallerFragmentContainer);
        if (fragment instanceof MilitaryFragment2) {
            MilitaryFragment2 frag2 = (MilitaryFragment2) fragment;
            //frag2.showCreateNote(null, false);
        }
        else if (fragment instanceof MilitaryFragment3) {
            MilitaryFragment3 frag3 = (MilitaryFragment3) fragment;
            frag3.rotateZoomLayout();
        }
        else if (fragment instanceof MilitaryFragment4) {
            MilitaryFragment4 frag4 = (MilitaryFragment4) fragment;
            frag4.showCreateTask();
        }
    }
}