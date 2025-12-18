import { useState } from "react";

function Slider({data, onValueChange}) {
    const disabled = !data || !data.length;
    const max = disabled ? 0 : data.length - 1;
    const [value, setValue] = useState(max);

    const handleChange = (e) => {
        const newValue = Number(e.target.value);
        setValue(newValue);
        onValueChange(data[newValue]);
    };

    return (
        <div className="w-full max-w-md mx-auto p-6">
            <label className="block mb-2 text-sm font-medium text-gray-700">
                {disabled ? "--" : data[value].label}
            </label>
            <input
                type="range"
                disabled={disabled}
                min="0"
                max={max}
                value={value}
                onChange={handleChange}
                className="
                    w-full
                    h-2
                    bg-gray-200
                    rounded-lg
                    appearance-none
                    cursor-pointer
                    accent-blue-600
                    disabled:opacity-60
                    disabled:bg-gray-100
                    disabled:border-gray-300
                    disabled:cursor-not-allowed" />
        </div>
    );
}

export default Slider;