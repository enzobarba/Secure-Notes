package com.example.securenotes.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentCreatePinBinding;
import com.example.securenotes.security.PinManager;

/*
Fragment per forzare l'utente a creare un PIN
al primo avvio.
*/
public class CreatePinFragment extends Fragment {

    // Interfaccia callback per notificare AuthActivity
    public interface PinCreationListener {
        void onPinCreated();
    }

    private FragmentCreatePinBinding binding;
    private PinCreationListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //Activity deve implementare il listener
        if (context instanceof PinCreationListener) {
            listener = (PinCreationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PinCreationListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatePinBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSavePin.setOnClickListener(v -> {
            String pin1 = binding.editTextPin.getText().toString();
            String pin2 = binding.editTextPinConfirm.getText().toString();

            // Logica di validazione e salvataggio
            if (pin1.length() < 4) {
                Toast.makeText(getContext(), R.string.settings_pin, Toast.LENGTH_SHORT).show();
            } else if (!pin1.equals(pin2)) {
                Toast.makeText(getContext(), R.string.pins_not_equal, Toast.LENGTH_SHORT).show();
            } else {
                // Chiama il PinManager per salvare l'hash
                PinManager.savePin(requireContext(), pin1);
                // Notifica l'activity che ha finito
                listener.onPinCreated();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}