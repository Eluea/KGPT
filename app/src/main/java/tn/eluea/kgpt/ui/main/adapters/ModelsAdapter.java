/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.main.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.model.CustomProvider;

public class ModelsAdapter extends RecyclerView.Adapter<ModelsAdapter.ModelViewHolder> {

    public static class ProviderItem {
        public enum Type { BUILTIN, CUSTOM }
        public final Type type;
        public final LanguageModel builtinModel;
        public final CustomProvider customProvider;

        public ProviderItem(LanguageModel builtinModel) {
            this.type = Type.BUILTIN;
            this.builtinModel = builtinModel;
            this.customProvider = null;
        }

        public ProviderItem(CustomProvider customProvider) {
            this.type = Type.CUSTOM;
            this.builtinModel = null;
            this.customProvider = customProvider;
        }
    }

    public interface OnModelSelectedListener {
        void onBuiltinModelSelected(LanguageModel model);
        void onCustomProviderSelected(CustomProvider provider);
        void onEditCustomProvider(CustomProvider provider);
    }

    private final List<ProviderItem> items = new ArrayList<>();
    private LanguageModel selectedBuiltinModel;
    private String selectedCustomProviderId;
    private final OnModelSelectedListener listener;

    public ModelsAdapter(List<LanguageModel> builtinModels,
                         List<CustomProvider> customProviders,
                         LanguageModel selectedBuiltinModel,
                         String selectedCustomProviderId,
                         OnModelSelectedListener listener) {
        this.selectedBuiltinModel = selectedBuiltinModel;
        this.selectedCustomProviderId = selectedCustomProviderId;
        this.listener = listener;
        updateData(builtinModels, customProviders, selectedBuiltinModel, selectedCustomProviderId);
    }

    private int findSelectedPosition() {
        for (int i = 0; i < items.size(); i++) {
            ProviderItem it = items.get(i);
            if (it.type == ProviderItem.Type.BUILTIN && it.builtinModel != null
                    && it.builtinModel.equals(selectedBuiltinModel)) return i;
            if (it.type == ProviderItem.Type.CUSTOM && it.customProvider != null
                    && it.customProvider.getId() != null
                    && it.customProvider.getId().equals(selectedCustomProviderId)) return i;
        }
        return -1;
    }

    public void updateData(List<LanguageModel> builtinModels,
                           List<CustomProvider> customProviders,
                           LanguageModel selectedBuiltinModel,
                           String selectedCustomProviderId) {
        this.selectedBuiltinModel = selectedBuiltinModel;
        this.selectedCustomProviderId = selectedCustomProviderId;
        this.items.clear();

        if (builtinModels != null) {
            for (LanguageModel m : builtinModels) {
                this.items.add(new ProviderItem(m));
            }
        }

        if (customProviders != null) {
            for (CustomProvider cp : customProviders) {
                this.items.add(new ProviderItem(cp));
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        ProviderItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ModelViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardModel;
        private final FrameLayout iconContainer;
        private final ImageView ivModelIconStatic;
        private final com.airbnb.lottie.LottieAnimationView lottieModelIcon;
        private final TextView tvModelName;
        ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            cardModel = itemView.findViewById(R.id.card_model);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivModelIconStatic = itemView.findViewById(R.id.iv_model_icon_static);
            lottieModelIcon = itemView.findViewById(R.id.lottie_model_icon);
            tvModelName = itemView.findViewById(R.id.tv_model_name);
        }

        void bind(ProviderItem item) {
            boolean isSelected;
            String label;

            if (item.type == ProviderItem.Type.BUILTIN) {
                label = item.builtinModel.label;
                isSelected = (selectedCustomProviderId == null && item.builtinModel == selectedBuiltinModel);
            } else {
                label = item.customProvider.getName();
                isSelected = (selectedCustomProviderId != null && selectedCustomProviderId.equals(item.customProvider.getId()));
            }

            tvModelName.setText(label);

            int colorPrimary = com.google.android.material.color.MaterialColors.getColor(itemView,
                    androidx.appcompat.R.attr.colorPrimary);
            int colorOnSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            int colorDivider = ContextCompat.getColor(itemView.getContext(), R.color.divider_color);

            if (isSelected) {
                cardModel.setStrokeColor(colorPrimary);
                cardModel.setStrokeWidth(3);
                if (ivModelIconStatic != null) ivModelIconStatic.setVisibility(View.GONE);
                if (lottieModelIcon != null) {
                    lottieModelIcon.setVisibility(View.VISIBLE);
                    tn.eluea.kgpt.util.LottieHelper.tint(lottieModelIcon, colorPrimary);
                    lottieModelIcon.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
                    if (!lottieModelIcon.isAnimating()) {
                        lottieModelIcon.playAnimation();
                    }
                }
            } else {
                cardModel.setStrokeColor(colorDivider);
                cardModel.setStrokeWidth(1);
                if (lottieModelIcon != null) {
                    lottieModelIcon.pauseAnimation();
                    lottieModelIcon.setVisibility(View.GONE);
                }
                if (ivModelIconStatic != null) {
                    ivModelIconStatic.setVisibility(View.VISIBLE);
                    ivModelIconStatic.setColorFilter(colorOnSurfaceVariant);
                }
            }

            cardModel.setOnClickListener(v -> {
                if (item.type == ProviderItem.Type.BUILTIN) {
                    int oldPos = findSelectedPosition();
                    selectedBuiltinModel = item.builtinModel;
                    selectedCustomProviderId = null;
                    // E4: rebind only the affected cells (full notify restarted
                    // every row's Lottie)
                    if (oldPos >= 0) notifyItemChanged(oldPos);
                    notifyItemChanged(getBindingAdapterPosition());
                    if (listener != null) {
                        listener.onBuiltinModelSelected(item.builtinModel);
                    }
                } else if (item.type == ProviderItem.Type.CUSTOM) {
                    int oldPos = findSelectedPosition();
                    selectedCustomProviderId = item.customProvider.getId();
                    selectedBuiltinModel = null;
                    if (oldPos >= 0) notifyItemChanged(oldPos);
                    notifyItemChanged(getBindingAdapterPosition());
                    if (listener != null) {
                        listener.onCustomProviderSelected(item.customProvider);
                    }
                }
            });

            // Long click on custom provider to edit/delete
            if (item.type == ProviderItem.Type.CUSTOM) {
                cardModel.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onEditCustomProvider(item.customProvider);
                    }
                    return true;
                });
            } else {
                cardModel.setOnLongClickListener(null);
            }
        }
    }
}
