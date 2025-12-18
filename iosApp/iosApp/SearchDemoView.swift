import SwiftUI

class SearchDemoViewModel: ObservableObject {
    @Published var searchText = ""
    @Published var searchResults: [String] = []
    @Published var selectedItems: [String] = []
    @Published var searchCount = 0

    private let allFruits = [
        "Apple", "Apricot", "Avocado",
        "Banana", "Blackberry", "Blueberry",
        "Cherry", "Coconut", "Cranberry",
        "Date", "Dragonfruit",
        "Elderberry",
        "Fig",
        "Grape", "Grapefruit", "Guava",
        "Honeydew",
        "Kiwi", "Kumquat",
        "Lemon", "Lime", "Lychee",
        "Mango", "Melon",
        "Nectarine",
        "Orange",
        "Papaya", "Passion Fruit", "Peach", "Pear", "Pineapple", "Plum", "Pomegranate",
        "Raspberry",
        "Strawberry",
        "Tangerine",
        "Watermelon"
    ]

    private var debouncer: SwiftDebouncer?
    private var currentQuery = ""

    init() {
        // Initialize debouncer with 300ms delay
        debouncer = SwiftDebouncer(delayMillis: 300) { [weak self] in
            self?.performSearch()
        }
    }

    func search(query: String) {
        currentQuery = query
        // Call debouncer instead of searching immediately
        debouncer?.call()
    }

    private func performSearch() {
        searchCount += 1

        if currentQuery.isEmpty {
            searchResults = []
        } else {
            searchResults = allFruits.filter { fruit in
                fruit.lowercased().contains(currentQuery.lowercased())
            }
        }
    }

    func selectItem(_ item: String) {
        if !selectedItems.contains(item) {
            selectedItems.append(item)
        }
        searchText = ""
        searchResults = []
    }

    func removeItem(_ item: String) {
        selectedItems.removeAll { $0 == item }
    }
}

struct SearchDemoView: View {
    @StateObject private var viewModel = SearchDemoViewModel()

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text("Autocomplete Search")
                    .font(.title2)
                    .bold()

                Text("Type to search fruits. Uses debounce (300ms) to avoid excessive API calls.")
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack {
                    Text("Search Count:")
                    Text("\(viewModel.searchCount)")
                        .bold()
                        .foregroundColor(.blue)
                }
                .font(.caption)
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemGroupedBackground))

            // Search Box
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.gray)

                TextField("Search fruits...", text: $viewModel.searchText)
                    .textFieldStyle(PlainTextFieldStyle())
                    .onChange(of: viewModel.searchText) { newValue in
                        // TODO: Add debounce here
                        viewModel.search(query: newValue)
                    }

                if !viewModel.searchText.isEmpty {
                    Button(action: { viewModel.searchText = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding()
            .background(Color(.systemBackground))

            // Search Results
            if !viewModel.searchResults.isEmpty {
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(viewModel.searchResults, id: \.self) { fruit in
                            Button(action: { viewModel.selectItem(fruit) }) {
                                HStack {
                                    Text(fruit)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    Image(systemName: "plus.circle")
                                        .foregroundColor(.blue)
                                }
                                .padding()
                            }
                            Divider()
                        }
                    }
                }
                .background(Color(.systemBackground))
            }

            // Selected Items
            if !viewModel.selectedItems.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Selected Items (\(viewModel.selectedItems.count))")
                        .font(.headline)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(viewModel.selectedItems, id: \.self) { item in
                                HStack(spacing: 4) {
                                    Text(item)
                                    Button(action: { viewModel.removeItem(item) }) {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.gray)
                                    }
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.blue.opacity(0.1))
                                .cornerRadius(16)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.systemGroupedBackground))
            }

            Spacer()
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SearchDemoView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SearchDemoView()
        }
    }
}
