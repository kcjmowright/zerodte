function SmallCheckboxButton({ id, label, checked, onChange }) {
    return (
        <div className="inline-flex items-center">
            <input
                type="checkbox"
                id={id}
                checked={checked}
                onChange={onChange}
                className="peer sr-only" />
            <label htmlFor={id}
                   className="cursor-pointer select-none rounded-xs bg-gray-200 px-1 py-0.5 font-medium text-gray-700 transition-colors hover:bg-gray-300 peer-checked:bg-blue-600 peer-checked:text-white">
                {label}
            </label>
        </div>
    );
}
export default SmallCheckboxButton;