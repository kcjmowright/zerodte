# React Basics

[React](https://react.dev) is an open-source JavaScript library for creating user interfaces, particularly suited for single-page applications. 
Created and maintained by Meta, it stands as one of the leading tools in frontend web development. React's fundamental 
approach centers on decomposing intricate user interfaces into smaller, self-contained building blocks known as components.

## Core Concepts

### Component-Based Architecture

Instead of writing one massive file for a webpage, you build individual components that are reusable 
and can be nested within each other.

### Declarative UI

You describe what you want the UI to look like for a given state and [React](https://react.dev) handles updating the browser when data changes.

### Virtual DOM

React keeps a lightweight copy of the webpage in memory (the Virtual DOM). 
When data changes, it compares the virtual copy to the real one and updates only the specific parts that changed, 
making it much faster than traditional methods.

### JSX (JavaScript XML)

React uses a syntax extension called JSX that looks like HTML but lives inside your JavaScript. 
It makes writing component structures more intuitive.

---

## Generating a New React Project

The recommended way to generate a [React](https://react.dev) project depends on whether you are building 
simple client-side app or a full-stack production application.

### Production & Full-Stack Apps

Frameworks are now the "default" recommendation because they handle routing, data fetching, and performance optimizations 
(like [Server Components](https://builder.aws.com/content/35mjuFWn4hSGCK6JjaZHFIGrzPG/reactjs-best-practices-in-2026)) 
out of the box.

#### [Next.js](https://www.google.com/search?q=https://react.dev/learn/creating-a-react-app%23nextjs-app-router)

The most popular choice for SEO-friendly, high-performance applications is Next.js. 
It uses the App Router to leverage the full power of React's latest architecture.

To create a [React](https://react.dev) project with Next.js, type:

`npx create-next-app@latest`

#### [React Router](https://www.google.com/search?q=https://react.dev/learn/creating-a-react-app%23react-router-v7)

React Router is a powerful full-stack framework emphasizing standard Web APIs. 

To create a new [React](https://react.dev) project using **[React Router](https://reactrouter.com/home)**, type:

`npx create-react-router@latest my-react-router-app`

where `my-react-router-app` is the name and folder where the [React](https://react.dev) router app archetype will be created.


#### [Expo](https://www.google.com/search?q=https://react.dev/learn/creating-a-react-app%23expo-for-native-apps)

Expo is for building universal apps that run on Android, iOS, and the web simultaneously.

To create a [React](https://react.dev) project with **[Expo](https://www.google.com/search?q=https://react.dev/learn/creating-a-react-app%23expo-for-native-apps)**, type:

`npx create-expo-app@latest`


### Generating Simple React Apps

If you don't need a full-stack framework and want a fast, lightweight setup, 
**[Vite](https://learningdaily.dev/create-react-app-cra-deprecated-whats-next-d0c3532958e8)** is the industry standard. 
It is highly recommended for internal tools, dashboards, or learning the basics of React.
**[Vite](https://learningdaily.dev/create-react-app-cra-deprecated-whats-next-d0c3532958e8)** provides near-instant Hot Module Replacement (HMR) and much faster build times than Webpack.

To create a react project, type:

`npm create vite@latest react-project -- --template react`

where `react-project` is the name of the folder where the react project will be created.

`cd react-project`

Examine the packages.json and remove the react project dependencies, then:

`npm install --save-exact react@rc react-dom@rc`

This ensures the latest version of react is installed.

---

## React Basics

### Variables and Properties

#### Javascript Variables

In JavaScript, variables are declared in various scopes using the following keywords.

- `var` - Is the old school way of declaring variables.  Variables declared using `var` are scoped to the surrounding function.
- `let` - Is the new way of declaring variables.  Variables declared using `let` are scoped to the surrounding brackets, `{}`, or module if no surrounding brackets exist.
- `const` - Short for constant, this is also the new way of declaring variables.  The difference between `let` and `const` is that `const` doesn't allow variable reassignment.
- Variables declared without a keyword are in the global scope which is contrary to best practices.

#### Component properties in React/JSX

Variables can be used inside the markup using brackets `{}`.
Components must have 1 wrapper tag.  
Components can have properties via the `props` object.  
Use destructuring to use only what you want from the `props` object.

```jsx
import './Dashboard.css'

// Note: `cmd ctrl space` opens the emoji keyboard on a Mac.
let bee = "📟";

function Header({name, year}) {
  return (
    <div>
      <h1>Hello {name}</h1>
      <p>Copyright {year}</p>
    </div>
  )
}

function Dashboard() {
  return (
    <div>
      <Header name={bee} year={new Date().getFullYear()} />
    </div>
  )
}

export default Dashboard
```

### Lists

Use the the `Array#map` function to iterate through elements.
React requires each item in the list to have a unique `key` property.  Its best to use an object with a unique id as shown above.

Double bracket `{{}}` to pass an object to a element property.

```jsx
const items = [
  "a",
  "b",
  "c"
];

const itemsObjs = items.map((item, i) => ({
    id: i,
    item: item
}));

function Main({dishes}) {
  return (
    <ul>
      { dishes.map((dish) => (
          <li key={dish.id} style={{ listStyleType: "none" }}>
            {dish.item}
          </li>
        ))
      }
    </ul>
  );
}

...
<Main dishes={itemsObjs} />

```

### Images

```jsx
import chef from "./images/chef.jpg";

...

<img src={chef} height={200} alt="A photo of a chef" />
```

### JSX Fragment

All components must be wrapped in a top level element.  `<React.Fragment>` is an alternative to cluttering the DOM with `div`s.  
Short-hand `<></>` removes the need to import `React`.

```jsx
import React from "docs/React";

<React.Fragment>
    ...
</React.Fragment>
```

```jsx
<>
...
</>
```

### Destructuring Arrays

```js
let [ one, two, three ] = ["one", "two", "three"];

console.log(one);

let [ , , three] = ["one", "two", "three"];

console.log(three);
```

### State Management with `useState`

```jsx
import {useState} from "docs/React";

function Dashboard() {
    const [status, setStatus] = useState(true);
    return (
        <div>
            <h1>The restaurant is currently {status ? "Open" : "Closed"}</h1>
            <button onClick={() => setStatus(!status)}>{status ? "Close" : "Open"} Restaurant</button>
        </div>
    );
}

```

### State Management with `useReducer`

The `useReducer` function allows the passage of a function reference.  Logic is contained within the `reducer`.

```jsx
import {useReducer} from "docs/React";

function Main({dishes, openStatus, onStatus}) {
    return (
        <>
            <button onClick={() => onStatus(true)}>
                I want to be open
            </button>
            <h2>
                Welcome to this beautiful restaurant!{" "}
                {openStatus ? "Open" : "Closed"}
            </h2>
        </>
    );
}

function Dashboard() {
    // const [status, setStatus] = useState(true);
    const [status, toggle] = useReducer((status) => !status, true);

    return (
        <div>
            <h1>
                The restaurant is currently{" "}
                {status ? "open" : "closed"}.
            </h1>
            <button onClick={toggle}>
                {status ? "Close" : "Open"} Restaurant
            </button>
            <Header name="Alex" year={new Date().getFullYear()}/>
            <Main
                dishes={dishObjects}
                openStatus={status}
                onStatus={toggle}
            />
        </div>
    );
}
```

### `useEffect`

```jsx

function Dashboard() {

 // By default, called each time `status` changes.
  useEffect(() => {
    console.log(`The restaurant status is ${status ? "open" : "closed" }`);
  });
}

```

By default, the `useEffect` hook is called everytime the value changes.  
Passing in an empty array, restricts the call to when its initialized.


```jsx

function Dashboard() {
  
  // Only called on initialization.
  useEffect(() => {
    console.log(`The restaurant status is ${status ? "open" : "closed" }`);
  }, []);
}

```

The second argument, the array, defines the dependencies of when this should be called.  An empty array defines no dependencies and therefore only fires on initialization.  Pass one or more variables and it fires whenever those variables change.

```jsx

function Dashboard() {
  
  // Only called on initialization.
  useEffect(() => {
    console.log(`The restaurant status is ${status ? "open" : "closed" }`);
  }, [status]);
}

```

---

## Fetching Data in a React Application

```jsx
async function getData() {
    const res = await fetch("htts://your-api-here");
    return res.json();
}

export default async function Page() {
    const data = await getData();
    return (
        <main>
            <pre>{JSON.stringify(data)}</pre>
        </main>
    )
}
```

---

## Forms

Use the JavaScript standard, [FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData).

Form action references an async function that maps the `FormData` to an object.

```jsx
export default function Page() {
  async function submitForm(formData) {
    "use server"; // 
    const formFields = {
      email: formData.get("email"),
      message: formData.get("message")
    };
    console.log("formFields", formFields);
    console.log(
      "TODO: Send these form field values to a backend"
    );
    return formFields;
  }
  return (
    <main className="max-w-md mx-auto p-6 bg-white shadow-md rounded-md">
      <h1 className="text-2xl font-bold text-center mb-6">
        Contact us!
      </h1>
      <form className="space-y-4" action={submitForm}>
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700"
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            name="email"
            required
            className="border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
        <div>
          <label
            htmlFor="message"
            className="block text-sm font-medium text-gray-700"
          >
            Message
          </label>
          <textarea
            id="message"
            required
            name="message"
            rows="4"
            className="border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          ></textarea>
        </div>
        <button
          type="submit"
          className="text-white bg-blue-600 rounded-md p-3"
        >
          Send Message
        </button>
      </form>
    </main>
  );
}
```

---

## Injecting Components into Components

In React, injecting or nesting components is a fundamental pattern used to build complex UIs from simple building blocks. 
There are three primary ways to achieve this depending on your specific needs.

### Direct Nesting

The most common method is to import a child component and place it directly within the JSX of a parent component. This creates a permanent, static relationship between the two.

```javascript
function Child() {
  return <p>I am the child component!</p>;
}

function Parent() {
  return (
    <div>
      <h1>Parent Container</h1>
      <Child /> 
    </div>
  );
}

```

### Using the `children` Prop

If you want to create a "wrapper" or "layout" component that can hold any content passed to it, use the [special children prop](https://www.google.com/search?q=https://react.dev/learn/passing-props-to-a-component%23passing-jsx-as-children). This is highly effective for sidebars, modals, or cards.

* **Flexible:** The parent doesn't need to know what's inside it ahead of time.
* **Syntax:** Content placed between the opening and closing tags of a component is automatically passed as `children`.

```javascript
function Card({ children }) {
  return <div className="card-style">{children}</div>;
}

function App() {
  return (
    <Card>
      <p>This text is "injected" into the Card via the children prop.</p>
    </Card>
  );
}

```

### Component Injection Using Props

You can pass a component (or a reference to one) as a standard prop. This is useful when a component needs to render something in a specific slot, like a "Header" or "Footer" area.

* **Slot Pattern:** Define specific locations where external components should appear.
* **Logic:** You can pass either a rendered element or a function that returns an element.

```javascript
function Layout({ header, content }) {
  return (
    <main>
      <header>{header}</header>
      <section>{content}</section>
    </main>
  );
}

<Layout 
  header={<Navbar />} 
  content={<Dashboard />} 
/>

```

---

## Optional Attributes in JSX Markup

In JSX, defining an optional HTML attribute is typically handled using **JavaScript logical operators** 
or **ternary expressions** directly within the curly braces of an attribute.

Here are the most common ways to achieve this:

### Using the Logical AND (`&&`) Operator

This is the cleanest method when you want an attribute to appear only if a condition is true. If the condition is false, [React](https://react.dev) will omit the attribute entirely.

```jsx
<button disabled={isDisabled && true}>
  Submit
</button>

```

### Passing `undefined` or `null`

React is designed to automatically omit any attribute whose value is `null` or `undefined`. This is very useful for optional props.

```jsx
<div id={userId || undefined}>
  User Profile
</div>

```

### Conditional Object Spreading

If you have multiple optional attributes, you can spread an object. This keeps your JSX tag from becoming cluttered with multiple ternary operators.

```jsx
const optionalAttributes = isPremium ? { 'data-status': 'gold', title: 'VIP User' } : {};

<div {...optionalAttributes}>
  Welcome back!
</div>

```

---

## Adding CSS Using Tailwind CSS


[Tailwind](https://tailwindcss.com/) is a CSS framework that compliments React.

`npm install tailwindcss @tailwindcss/vite`

## References

* [React Developer Website](https://react.dev)
* [Official React Documentation](https://react.dev/learn/creating-a-react-app)
* [React Playground](https://playground.react.dev)
* [Using the Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)
