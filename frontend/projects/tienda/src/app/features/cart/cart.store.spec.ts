/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { TestBed } from '@angular/core/testing';
import { CartStore } from './cart.store';

describe('CartStore', () => {
  function store(): CartStore {
    localStorage.clear();
    TestBed.resetTestingModule();
    return TestBed.inject(CartStore);
  }

  it('starts empty', () => {
    const cart = store();

    expect(cart.count()).toBe(0);
    expect(cart.subtotal()).toBe(0);
    expect(cart.canCheckout()).toBe(false);
  });

  it('adds products and adds up units and amounts to the cent', () => {
    const cart = store();

    cart.add('leche-entera-1l', 3);
    cart.add('aove-1l', 1);
    cart.add('leche-entera-1l');

    expect(cart.count()).toBe(5);
    expect(cart.quantityOf('leche-entera-1l')).toBe(4);
    expect(cart.subtotal()).toBe(12.75);
    expect(cart.items().map((item) => item.total)).toEqual([3.8, 8.95]);
  });

  it('ignores products that do not exist and never goes past the stock', () => {
    const cart = store();

    cart.add('no-existe');
    cart.add('lechuga-romana', 10);
    cart.add('merluza-rodajas-600g');

    expect(cart.quantityOf('no-existe')).toBe(0);
    expect(cart.quantityOf('lechuga-romana')).toBe(3);
    expect(cart.quantityOf('merluza-rodajas-600g')).toBe(0);
  });

  it('changes and removes lines', () => {
    const cart = store();
    cart.add('leche-entera-1l', 2);

    cart.setQuantity('leche-entera-1l', 5);
    expect(cart.quantityOf('leche-entera-1l')).toBe(5);

    cart.setQuantity('leche-entera-1l', 0);
    expect(cart.count()).toBe(0);

    cart.add('aove-1l');
    cart.remove('aove-1l');
    expect(cart.count()).toBe(0);
  });

  it('computes the delivery cost when a zone is chosen', () => {
    const cart = store();
    cart.add('aove-5l', 1);
    expect(cart.deliveryFee()).toBeNull();

    cart.selectZone('competa');
    expect(cart.deliveryFee()).toBe(4.5);
    expect(cart.total()).toBe(44.4);

    cart.add('aove-5l', 1);
    expect(cart.deliveryFee()).toBe(0);
    expect(cart.total()).toBe(79.8);
  });

  it('does not allow checking out outside the delivery area or below the minimum order', () => {
    const cart = store();
    cart.add('leche-entera-1l', 1);
    cart.selectZone('competa');
    expect(cart.belowMinimum()).toBe(true);
    expect(cart.canCheckout()).toBe(false);

    cart.add('aove-5l', 1);
    expect(cart.canCheckout()).toBe(true);

    cart.selectZone('malaga');
    expect(cart.delivery()).toEqual({ kind: 'out-of-zone' });
    expect(cart.canCheckout()).toBe(false);
  });

  it('knows when the cart has products for adults only', () => {
    const cart = store();
    cart.add('leche-entera-1l');
    expect(cart.hasAdultsOnly()).toBe(false);

    cart.add('cerveza-rubia-6');
    expect(cart.hasAdultsOnly()).toBe(true);
  });

  it('remembers the cart between visits', async () => {
    const cart = store();
    cart.add('aove-1l', 2);
    cart.selectZone('periana');
    TestBed.tick();

    TestBed.resetTestingModule();
    const again = TestBed.inject(CartStore);

    expect(again.quantityOf('aove-1l')).toBe(2);
    expect(again.zone()?.slug).toBe('periana');
  });

  it('starts empty when what was saved is damaged', () => {
    localStorage.setItem('lv_demo_cart', '{esto no es json');
    TestBed.resetTestingModule();

    expect(TestBed.inject(CartStore).count()).toBe(0);
  });

  it('opens and closes the side panel', () => {
    const cart = store();

    cart.openDrawer();
    expect(cart.drawerOpen()).toBe(true);

    cart.closeDrawer();
    expect(cart.drawerOpen()).toBe(false);
  });
});
