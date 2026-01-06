package com.app.ectrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.ViewHolder> {

    private List<Prescription> prescriptions = new ArrayList<>();

    public void setPrescriptions(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prescription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription prescription = prescriptions.get(position);
        holder.bind(prescription);
    }

    @Override
    public int getItemCount() {
        return prescriptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textPatientName, textDate, textMedicineName;
        TextView textDosage, textDuration, textBoxQuantity;

        ViewHolder(View itemView) {
            super(itemView);
            textPatientName = itemView.findViewById(R.id.textPatientName);
            textDate = itemView.findViewById(R.id.textDate);
            textMedicineName = itemView.findViewById(R.id.textMedicineName);
            textDosage = itemView.findViewById(R.id.textDosage);
            textDuration = itemView.findViewById(R.id.textDuration);
            textBoxQuantity = itemView.findViewById(R.id.textBoxQuantity);
        }

        void bind(Prescription prescription) {
            textPatientName.setText(prescription.getPatientName());
            textMedicineName.setText(prescription.getMedicineName());
            textDosage.setText("Doz: " + prescription.getDosage());
            textDuration.setText("Süre: " + prescription.getDuration());
            textBoxQuantity.setText("Kutu: " + prescription.getBoxQuantity());

            if (prescription.getPrescribedDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                textDate.setText(sdf.format(new Date(prescription.getPrescribedDate().getSeconds() * 1000)));
            }
        }
    }
}

