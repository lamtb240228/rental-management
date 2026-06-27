import { create } from "zustand";

type PropertyUiState = {
  selectedPropertyId: number | null;
  setSelectedPropertyId: (id: number | null) => void;
};

export const usePropertyUiStore = create<PropertyUiState>((set) => ({
  selectedPropertyId: null,
  setSelectedPropertyId: (selectedPropertyId) => set({ selectedPropertyId }),
}));
