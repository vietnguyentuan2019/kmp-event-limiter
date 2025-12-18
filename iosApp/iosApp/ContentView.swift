import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationView {
            List {
                NavigationLink("Search Demo", destination: SearchDemoView())
                NavigationLink("Form Validation Demo", destination: FormDemoView())
                NavigationLink("Payment Protection Demo", destination: PaymentDemoView())
                NavigationLink("Infinite Scroll Demo", destination: InfiniteScrollDemoView())
                NavigationLink("Settings & Playground", destination: SettingsDemoView())
            }
            .navigationTitle("KMP Event Limiter")
            .listStyle(InsetGroupedListStyle())
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
