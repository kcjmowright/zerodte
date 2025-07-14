import ZeroDteIronCondor from "./ZeroDteIronCondor.jsx";

function Dashboard() {

  return (
      <>
          <header className="bg-white shadow-sm">
              <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                  <h1 className="text-3xl font-bold tracking-tight text-gray-900">Dashboard</h1>
              </div>
          </header>
          <main>
              <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                  <ZeroDteIronCondor inSymbol="QQQ" />
              </div>
          </main>
      </>
  )
}

export default Dashboard;
