const fs = require('fs');
const parsersCode = fs.readFileSync('c:/Users/T480s/Documents/UML GENERATOR/web/parsers.js', 'utf8');

// evaluate parsers
let parsers;
eval(parsersCode.replace('const parsers =', 'parsers ='));

const sampleCode = `
public class Checkout {
    private ToggleGroup paymentgroup;
    public Checkout() {}
}
class Buyers {
    public Buyers() {}
}
class ShoppingCart {
    private List items;
}
`;

const diagram = parsers.java.parse(sampleCode);
console.log(JSON.stringify(diagram.classes, null, 2));
