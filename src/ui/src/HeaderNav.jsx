import { NavLink } from "react-router";

function HeaderNav() {
    return (
        <nav className="bg-gray-800">
            <div className="flex flex-1 items-center justify-center sm:items-stretch sm:justify-start">
                <div className="hidden sm:ml-6 sm:block">
                    <div className="flex space-x-4">
                        <NavLink to="/"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Home</NavLink>
                        <NavLink to="/account"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Account</NavLink>
                        <NavLink to="/orders"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Orders</NavLink>
                        <NavLink to="/quote"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Quote</NavLink>
                        <NavLink to="/movers"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Movers</NavLink>
                        <NavLink to="/login"
                                 className={({isActive}) => isActive ? "rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white"
                                     : "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}>Login</NavLink>
                    </div>
                </div>
            </div>
        </nav>
    );
}

export default HeaderNav;