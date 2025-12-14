const formatters = {
    number: new Intl.NumberFormat('en-US'),
    currency: new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }),
    percentage: new Intl.NumberFormat('en-US', {
        style: 'percent',
        minimumFractionDigits: 2
    })
};

export default formatters;