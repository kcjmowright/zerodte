import {useEffect, useState} from "react";

function Login() {
    const [username, setUsername] = useState(null);

    async function fetchSessionStatus() {
        try {
            const response = await fetch("/api/v1/session/status");
            const status = await response.json();
            console.log(status);
            if (status) {
                setUsername(status.username);
            }
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    }

    useEffect(() => {
        (() => fetchSessionStatus())();
    }, []);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900">Login</h1>
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                {
                    (() => {
                        if (!username) {
                            return <form method="get" action="/oauth2/schwab/authorization">
                                    <input type="hidden" name="callback" value="https://127.0.0.1:8443"/>
                                    <input autoFocus type="text" id="schwabUserId" name="schwabUserId" placeholder="User name"/>
                                    <button type="submit" className="text-white bg-gray-400 rounded-md p-1">Login</button>
                                </form>
                        }
                        return <div>Hello {username}</div>
                    })()
                }
                </div>
            </main>
        </>
    );
}

export default Login;
