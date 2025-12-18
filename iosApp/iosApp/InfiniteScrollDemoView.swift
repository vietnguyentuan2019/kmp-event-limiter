import SwiftUI

struct Post: Identifiable {
    let id: Int
    let title: String
    let body: String
}

class InfiniteScrollViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var loadAttempts = 0
    @Published var actualLoads = 0
    @Published var currentPage = 0

    private let totalPages = 10
    private let postsPerPage = 10
    private var throttler: SwiftThrottler?

    var hasMore: Bool {
        currentPage < totalPages
    }

    var blockedAttempts: Int {
        loadAttempts - actualLoads
    }

    init() {
        // Initialize throttler with 2000ms delay (2 seconds)
        throttler = SwiftThrottler(delayMillis: 2000) { [weak self] in
            self?.performLoad()
        }
    }

    func loadMore() {
        loadAttempts += 1

        guard !isLoading && hasMore else {
            return
        }

        // Call throttler to prevent excessive loads
        throttler?.call()
    }

    private func performLoad() {
        guard !isLoading && hasMore else {
            return
        }

        isLoading = true
        actualLoads += 1

        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            guard let self = self else { return }

            let startId = self.currentPage * self.postsPerPage + 1
            let newPosts = (startId..<(startId + self.postsPerPage)).map { id in
                Post(
                    id: id,
                    title: "Post #\(id)",
                    body: "This is the content of post #\(id). It demonstrates infinite scrolling with throttling."
                )
            }

            self.posts.append(contentsOf: newPosts)
            self.currentPage += 1
            self.isLoading = false
        }
    }

    func reset() {
        posts.removeAll()
        currentPage = 0
        loadAttempts = 0
        actualLoads = 0
    }
}

struct InfiniteScrollDemoView: View {
    @StateObject private var viewModel = InfiniteScrollViewModel()

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text("Infinite Scroll")
                    .font(.title2)
                    .bold()

                Text("Scroll to the bottom to load more posts. Throttle (2s) prevents excessive API calls.")
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack(spacing: 20) {
                    VStack(alignment: .leading) {
                        Text("Load Attempts")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(viewModel.loadAttempts)")
                            .font(.headline)
                            .foregroundColor(.blue)
                    }

                    VStack(alignment: .leading) {
                        Text("Actual Loads")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(viewModel.actualLoads)")
                            .font(.headline)
                            .foregroundColor(.green)
                    }

                    VStack(alignment: .leading) {
                        Text("Blocked")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(viewModel.blockedAttempts)")
                            .font(.headline)
                            .foregroundColor(.orange)
                    }
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemGroupedBackground))

            // Posts List
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(viewModel.posts) { post in
                        postRow(post)
                        Divider()
                    }

                    // Load More Trigger
                    if viewModel.hasMore {
                        loadMoreView
                            .onAppear {
                                viewModel.loadMore()
                            }
                    } else {
                        endOfListView
                    }
                }
            }

            // Reset Button
            Button(action: { viewModel.reset() }) {
                Text("Reset")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .foregroundColor(.red)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if viewModel.posts.isEmpty {
                viewModel.loadMore()
            }
        }
    }

    private func postRow(_ post: Post) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(post.title)
                .font(.headline)
            Text(post.body)
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var loadMoreView: some View {
        VStack(spacing: 12) {
            if viewModel.isLoading {
                ProgressView()
                Text("Loading more posts...")
                    .font(.caption)
                    .foregroundColor(.gray)
            } else {
                Button(action: { viewModel.loadMore() }) {
                    Text("Load More")
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.blue.opacity(0.1))
                        .foregroundColor(.blue)
                        .cornerRadius(8)
                }
                .padding()
            }
        }
        .padding()
    }

    private var endOfListView: some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.circle")
                .font(.system(size: 40))
                .foregroundColor(.green)
            Text("End of List")
                .font(.headline)
            Text("You've loaded all \(viewModel.posts.count) posts!")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(40)
    }
}

struct InfiniteScrollDemoView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            InfiniteScrollDemoView()
        }
    }
}
