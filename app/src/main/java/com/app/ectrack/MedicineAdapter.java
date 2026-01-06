package com.app.ectrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<Medicine> medicines = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Medicine medicine);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setMedicines(List<Medicine> medicines) {
        this.medicines = medicines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicines.get(position);
        holder.bind(medicine);
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {
        private TextView textName;
        private TextView textPrice;
        private TextView textStock;
        private TextView textExpiry;
        private TextView textDescription;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textPrice = itemView.findViewById(R.id.textPrice);
            textStock = itemView.findViewById(R.id.textStock);
            textExpiry = itemView.findViewById(R.id.textExpiry);
            textDescription = itemView.findViewById(R.id.textDescription);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(medicines.get(position));
                }
            });
        }

        public void bind(Medicine medicine) {
            textName.setText(medicine.getName());
            textPrice.setText(String.format(Locale.getDefault(), "%.2f TL", medicine.getPrice()));
            textStock.setText("Stok: " + medicine.getStock());

            if (medicine.getDescription() != null && !medicine.getDescription().isEmpty()) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(medicine.getDescription());
            } else {
                textDescription.setVisibility(View.GONE);
            }

            if (medicine.getExpiryDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                textExpiry.setText("SKT: " + sdf.format(medicine.getExpiryDate().toDate()));
            } else {
                textExpiry.setText("SKT: -");
            }
        }
    }
}

