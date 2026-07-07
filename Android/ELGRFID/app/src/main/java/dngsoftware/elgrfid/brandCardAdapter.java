package dngsoftware.elgrfid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class brandCardAdapter extends RecyclerView.Adapter<brandCardAdapter.ViewHolder> {

    public interface OnBrandSelectedListener {
        void onBrandSelected(PrinterBrand brand);
    }

    private final List<PrinterBrand> brands;
    private final OnBrandSelectedListener listener;

    public brandCardAdapter(List<PrinterBrand> brands, OnBrandSelectedListener listener) {
        this.brands = brands;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brand_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrinterBrand brand = brands.get(position);
        holder.name.setText(brand.nameRes);
        holder.description.setText(brand.descRes);
        int accent = ContextCompat.getColor(holder.itemView.getContext(), brand.accentColorRes);
        holder.accent.setBackgroundColor(accent);
        holder.card.setStrokeColor(accent);
        holder.card.setOnClickListener(v -> listener.onBrandSelected(brand));
    }

    @Override
    public int getItemCount() {
        return brands.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View accent;
        final TextView name;
        final TextView description;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.brand_card);
            accent = itemView.findViewById(R.id.brand_accent);
            name = itemView.findViewById(R.id.brand_name);
            description = itemView.findViewById(R.id.brand_description);
        }
    }
}