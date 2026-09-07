  // Navbar
  const header=document.getElementById('header');
  addEventListener('scroll',()=>header.classList.toggle('scrolled',scrollY>40));
  document.getElementById('hamburguesa').onclick=()=>document.getElementById('nav').classList.toggle('abierto');
  document.querySelectorAll('#nav a').forEach(a=>a.addEventListener('click',()=>document.getElementById('nav').classList.remove('abierto')));

  // Reveal on scroll
  const obs=new IntersectionObserver(es=>es.forEach(e=>{if(e.isIntersecting){e.target.classList.add('visible');obs.unobserve(e.target)}}),{threshold:.12});
  document.querySelectorAll('.revelar').forEach(el=>obs.observe(el));

  // Buscador hero -> scroll a habitaciones y setear fechas
  document.getElementById('buscador').addEventListener('submit',e=>{
    e.preventDefault();
    const en=document.getElementById('entrada').value, sa=document.getElementById('salida').value;
    document.getElementById('habitaciones').scrollIntoView({behavior:'smooth'});
    if(en&&sa){ document.getElementById('rEntrada').value=en; document.getElementById('rSalida').value=sa; }
  });

  // Reserva
  let tarifaActual=90, habitacionActual='Habitación Simple';
  const modal=document.getElementById('modalReserva');
  const TARIFAS={simple:90,doble:140,matrimonial:180};
  function cargarTarifas(){
    try{ const t=JSON.parse(localStorage.getItem('tampur_tarifas')); if(t){ Object.assign(TARIFAS,t); } }catch(e){}
    pintarPrecios();
  }

  // ===== Conexión con los microservicios =====
  const API_RESERVAS=(window.HOTEL_CONFIG&&window.HOTEL_CONFIG.reservas)||'http://localhost:8081';
  const API_PAGOS=(window.HOTEL_CONFIG&&window.HOTEL_CONFIG.pagos)||'http://localhost:8082';
  const API_BACKOFFICE=(window.HOTEL_CONFIG&&window.HOTEL_CONFIG.backoffice)||'http://localhost:8083';

  async function cargarDisponibilidad(){
    try{
      const res=await fetch(API_RESERVAS+'/api/habitaciones');
      if(!res.ok) return;
      const rooms=await res.json();
      const precios={};
      rooms.forEach(r=>{ if(!precios[r.tipo]) precios[r.tipo]=r.precioNoche; });
      if(precios['Simple']) TARIFAS.simple=precios['Simple'];
      if(precios['Doble']) TARIFAS.doble=precios['Doble'];
      if(precios['Matrimonial']) TARIFAS.matrimonial=precios['Matrimonial'];
      cargarTarifas();
    }catch(e){}
  }

  // ===== Conversión de moneda (para huéspedes extranjeros) =====
  const MONEDAS=[
    {code:'PEN',simbolo:'S/',nombre:'Soles peruanos'},
    {code:'USD',simbolo:'US$',nombre:'Dólar estadounidense'},
    {code:'EUR',simbolo:'€',nombre:'Euro'},
    {code:'GBP',simbolo:'£',nombre:'Libra esterlina'},
    {code:'BRL',simbolo:'R$',nombre:'Real brasileño'},
    {code:'MXN',simbolo:'MX$',nombre:'Peso mexicano'},
    {code:'ARS',simbolo:'AR$',nombre:'Peso argentino'},
    {code:'CLP',simbolo:'CL$',nombre:'Peso chileno'}
  ];
  let monedaActual='PEN';
  let tasasMoneda={PEN:1};

  /** Carga los tipos de cambio (base PEN) desde una API gratuita, con caché de 6 horas. */
  async function cargarTasas(){
    try{
      const cache=JSON.parse(localStorage.getItem('tampur_tasas'));
      if(cache && cache.rates && (Date.now()-cache.t)<6*3600*1000){
        tasasMoneda=cache.rates;
      } else {
        const res=await fetch('https://open.er-api.com/v6/latest/PEN');
        if(!res.ok) throw new Error('bad');
        const data=await res.json();
        if(data && data.result==='success' && data.rates){
          tasasMoneda=data.rates;
          localStorage.setItem('tampur_tasas',JSON.stringify({rates:data.rates,t:Date.now()}));
        }
      }
      pintarPrecios();
      actualizarPistaMoneda();
    }catch(e){}
  }

  /** Convierte un monto en soles a la moneda seleccionada y lo formatea. */
  function formatearPrecio(pen){
    const valor=pen*(tasasMoneda[monedaActual]||1);
    const maxDec=valor>=1000?0:(valor>=100?0:2);
    const numero=valor.toLocaleString('es-PE',{minimumFractionDigits:0,maximumFractionDigits:maxDec});
    const m=MONEDAS.find(x=>x.code===monedaActual);
    return (m?m.simbolo+' ':'')+numero;
  }

  function actualizarPistaMoneda(){
    const hint=document.getElementById('tasaHint');
    if(!hint) return;
    if(monedaActual==='PEN'){ hint.textContent=''; return; }
    const m=MONEDAS.find(x=>x.code===monedaActual);
    const porUnidad=1/(tasasMoneda[monedaActual]||1);
    hint.textContent='1 '+(m?m.simbolo:'')+' ≈ S/ '+porUnidad.toFixed(2);
  }

  function pintarPrecios(){
    const b=document.querySelectorAll('.tarjeta .precio b');
    const precios=[TARIFAS.simple,TARIFAS.doble,TARIFAS.matrimonial];
    b.forEach((el,i)=>{ if(precios[i]!=null) el.textContent=formatearPrecio(precios[i]); });
  }

  function refrescarTotal(){
    document.getElementById('rTotal').textContent='S/ '+totalActual;
    const conv=document.getElementById('rTotalConv');
    if(conv) conv.textContent=(monedaActual!=='PEN'&&totalActual>0)?('≈ '+formatearPrecio(totalActual)):'';
  }

  function iniciarMoneda(){
    const sel=document.getElementById('selMoneda');
    if(!sel) return;
    const guardada=localStorage.getItem('tampur_moneda');
    if(guardada){ monedaActual=guardada; sel.value=guardada; }
    sel.addEventListener('change',()=>{
      monedaActual=sel.value;
      localStorage.setItem('tampur_moneda',monedaActual);
      pintarPrecios();
      actualizarPistaMoneda();
    });
  }

  // ===== Animación de contadores (números que suben al entrar en pantalla) =====
  function animarContadores(){
    const els=document.querySelectorAll('[data-contar]');
    if(!els.length) return;
    const io=new IntersectionObserver(entradas=>{
      entradas.forEach(e=>{
        if(!e.isIntersecting) return;
        const el=e.target;
        const objetivo=parseFloat(el.dataset.contar)||0;
        const dur=1300, inicio=performance.now();
        const tick=ahora=>{
          const p=Math.min((ahora-inicio)/dur,1);
          const suave=1-Math.pow(1-p,3);
          el.textContent=Math.round(objetivo*suave).toLocaleString('es-PE');
          if(p<1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
        io.unobserve(el);
      });
    },{threshold:.5});
    els.forEach(el=>io.observe(el));
  }

  // Identificador único por cada intento de reserva. Se regenera cada vez que
  // se abre el modal, y viaja al backend para que este pueda detectar si el
  // mismo intento llega más de una vez (doble clic, doble envío, reintento).
  let idempotencyKey = null;
  function nuevaIdempotencyKey(){
    return (crypto.randomUUID ? crypto.randomUUID() : 'idk-'+Date.now()+'-'+Math.random().toString(36).slice(2));
  }

  // Evita que se dispare una segunda petición mientras la primera sigue en curso.
  let reservaEnProceso=false;

  function abrirReserva(nombre,key){
    habitacionActual=nombre; tarifaActual=TARIFAS[key]||90;
    idempotencyKey=nuevaIdempotencyKey();
    reservaEnProceso=false;
    document.getElementById('rTitulo').textContent=nombre;
    document.getElementById('rSubtitulo').textContent='Tarifa: S/ '+tarifaActual+' por noche';
    document.getElementById('rDetalle').textContent='Seleccione fechas';
    document.getElementById('rTotal').textContent='S/ 0';
    const conv0=document.getElementById('rTotalConv'); if(conv0) conv0.textContent='';
    totalActual=0;
    modal.classList.add('abierto');
    calcular();
  }
  function cerrarModal(){ modal.classList.remove('abierto'); }
  modal.addEventListener('click',e=>{ if(e.target===modal) cerrarModal(); });

  let totalActual=0;
  ['rEntrada','rSalida'].forEach(id=>document.getElementById(id).addEventListener('change',calcular));
  function calcular(){
    const en=document.getElementById('rEntrada').value, sa=document.getElementById('rSalida').value;
    if(en&&sa){
      const d1=new Date(en), d2=new Date(sa);
      const noches=Math.round((d2-d1)/(1000*60*60*24));
      if(noches>0){
        totalActual=noches*tarifaActual;
        document.getElementById('rDetalle').textContent=noches+' noche'+(noches>1?'s':'')+' · '+habitacionActual;
        refrescarTotal();
        if(document.getElementById('qrBox').style.display==='block') generarQrYape();
      } else { totalActual=0; document.getElementById('rDetalle').textContent='La salida debe ser posterior'; refrescarTotal(); }
    }
  }

  function generarQrYape(){
    const cont=document.getElementById('qrCode');
    const data='943370504';
    if(typeof qrcode==='undefined'){ cont.innerHTML='<span style="color:#888;font-size:.85rem">Generador de QR no disponible.</span>'; return; }
    const qr=qrcode(0,'M'); qr.addData(data); qr.make();
    cont.innerHTML=qr.createSvgTag({cellSize:5,margin:1});
    document.getElementById('qrMonto').textContent='S/ '+totalActual;
  }
  function actualizarMetodoPago(){
    const m=document.getElementById('rMetodo').value;
    const box=document.getElementById('qrBox');
    if(m==='yape'||m==='plin'){ box.style.display='block'; generarQrYape(); }
    else { box.style.display='none'; }
  }
  document.getElementById('rMetodo').addEventListener('change', actualizarMetodoPago);
  /** Valida los campos del formulario de reserva antes de enviarlo. Devuelve el primer error, o null si todo está bien. */
  function validarFormularioReserva({nombre,dni,correo,en,sa}){
    if(!nombre) return 'Ingrese su nombre completo.';
    if(!dni || dni.length<6) return 'Ingrese un DNI o pasaporte válido.';
    if(correo && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo)) return 'El correo ingresado no es válido.';
    if(!en || !sa) return 'Seleccione fecha de llegada y de salida.';
    if(new Date(sa) <= new Date(en)) return 'La fecha de salida debe ser posterior a la de llegada.';
    return null;
  }

  async function confirmarReserva(){
    // Guard: si ya hay una reserva en curso (por ejemplo, el usuario hizo doble clic),
    // se ignoran los clics adicionales en vez de disparar peticiones repetidas.
    if(reservaEnProceso) return;

    const nombre=document.getElementById('rNombre').value.trim();
    const dni=document.getElementById('rDni').value.trim();
    const correo=document.getElementById('rCorreo').value.trim();
    const tel=document.getElementById('rTelefono').value.trim();
    const metodo=document.getElementById('rMetodo').value;
    const voucherFile=document.getElementById('rVoucher').files[0];
    const voucherUrl=voucherFile?voucherFile.name:null;
    const en=document.getElementById('rEntrada').value, sa=document.getElementById('rSalida').value;

    const errorValidacion=validarFormularioReserva({nombre,dni,correo,en,sa});
    if(errorValidacion){ alert(errorValidacion); return; }

    const btn=document.getElementById('btnConfirmarReserva');
    reservaEnProceso=true;
    if(btn){ btn.disabled=true; btn.textContent='Procesando...'; }

    let codigo, total, noches, voucher='', pagoEstado='', desdeServidor=false, esDuplicada=false;
    let respuesta;
    try{
      const ctrl=new AbortController();
      const timer=setTimeout(()=>ctrl.abort(),90000);
      respuesta=await fetch(API_RESERVAS+'/api/reservas',{
        method:'POST', headers:{'Content-Type':'application/json'},
        body:JSON.stringify({
          tipoHabitacion:habitacionActual.replace('Habitación ',''),
          nombre, dni, correo, telefono:tel,
          fechaEntrada:en, fechaSalida:sa,
          idempotencyKey
        }),
        signal:ctrl.signal
      });
      clearTimeout(timer);
    }catch(e){
      respuesta=null;
    }

    if(respuesta && respuesta.status===400){
      // Datos inválidos según el backend: se le pide al huésped corregir, no se simula nada.
      const data=await respuesta.json().catch(()=>null);
      const mensaje=data && data.errores ? data.errores.join('\n') : 'Revise los datos ingresados.';
      alert(mensaje);
      reservaEnProceso=false;
      if(btn){ btn.disabled=false; btn.textContent='Confirmar reserva'; }
      return;
    }

    if(respuesta && respuesta.status===409){
      // El backend detectó que ya existe una reserva activa con estos mismos datos.
      const data=await respuesta.json();
      const existente=data.reservaExistente;
      codigo=existente.codigo; total=existente.total; noches=existente.noches;
      desdeServidor=true; esDuplicada=true;
    } else if(respuesta && respuesta.ok){
      const data=await respuesta.json();
      codigo=data.codigo; total=data.total; noches=data.noches; desdeServidor=true;

      try{
        const pr=await fetch(API_PAGOS+'/api/pagos',{
          method:'POST', headers:{'Content-Type':'application/json'},
          body:JSON.stringify({codigoReserva:codigo, metodo:metodo, voucherUrl:voucherUrl, nombre:nombre, correo:correo, monto:total})
        });
        if(pr.ok){ const pd=await pr.json(); voucher=pd.voucher; pagoEstado=pd.estado; }
      }catch(e){ pagoEstado='pago no procesado'; }
    } else {
      // Backend no disponible (respuesta=null) o error inesperado del servidor: modo local de respaldo.
      const d1=new Date(en), d2=new Date(sa);
      noches=Math.max(1,Math.round((d2-d1)/(1000*60*60*24)));
      total=noches*tarifaActual;
      codigo='TMP-'+Math.random().toString(36).slice(2,8).toUpperCase();
      const reservasLocales=JSON.parse(localStorage.getItem('tampur_reservas')||'[]');
      reservasLocales.push({codigo,habitacion:habitacionActual,llegada:en,salida:sa,noches,total,nombre,dni,correo,tel,estado:'Pendiente',creado:new Date().toLocaleString('es-PE')});
      localStorage.setItem('tampur_reservas',JSON.stringify(reservasLocales));
    }

    document.getElementById('modalReserva').innerHTML=`
      <div class="caja">
        <button class="cerrar" onclick="cerrarModal()">&times;</button>
        <div class="alerta-exito">
          <h3 style="margin-bottom:6px">${esDuplicada?'Ya tenías esta reserva registrada':'¡Reserva registrada!'}</h3>
          <p>${esDuplicada?'Encontramos una reserva activa con los mismos datos, así que no se creó una duplicada.':(desdeServidor?'Procesada por el servidor de reservas.':'Modo local (backend no conectado).')}</p>
          <div class="codigo">${codigo}</div>
          ${voucher?`<p class="sub">Voucher: <b>${voucher}</b> · Pago: ${pagoEstado}</p>`:''}
          <p class="sub">Check-in: 1:00 p.m. · Check-out: 12:00 p.m.</p>
          <button class="btn btn-primario" onclick="cerrarModal()">Listo</button>
        </div>
      </div>`;

    // El modal ya cambió de contenido (botón "Listo" en su lugar), así que no
    // hace falta reactivar btnConfirmarReserva: para una nueva reserva se
    // vuelve a llamar a abrirReserva(), que genera una idempotencyKey nueva.
  }

  // Formulario consulta
  document.getElementById('formConsulta').addEventListener('submit',e=>{
    e.preventDefault();
    const n=document.getElementById('cNombre').value, c=document.getElementById('cCorreo').value;
    if(!n||!c){ alert('Complete nombre y correo'); return; }
    const msg='Hola, soy '+encodeURIComponent(n)+'. '+encodeURIComponent(document.getElementById('cMensaje').value||'Quisiera información del hotel.');
    window.open('https://wa.me/51943370504?text='+msg,'_blank');
  });

  // ===== ADMIN =====
  function tokenAdmin(){ return sessionStorage.getItem('tampur_admin_token'); }
  function headersAdmin(){ const t=tokenAdmin(); const h={'Content-Type':'application/json'}; if(t){ h.Authorization='Bearer '+t; } return h; }
  function abrirAdmin(){
    if(tokenAdmin()){ entrarDash(); }
    else { document.getElementById('adminLogin').classList.add('abierto'); }
  }
  function cerrarAdmin(){
    document.getElementById('adminLogin').classList.remove('abierto');
    document.getElementById('adminDash').classList.remove('abierto');
  }
  async function loginAdmin(){
    const u=document.getElementById('aUsuario').value.trim();
    const c=document.getElementById('aClave').value;
    const errEl=document.getElementById('aError');
    if(!u||!c){ errEl.textContent='Ingrese usuario y contraseña'; return; }
    try{
      const res=await fetch(API_BACKOFFICE+'/api/admin/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({usuario:u,clave:c})});
      if(res.ok){
        const data=await res.json();
        sessionStorage.setItem('tampur_admin_token',data.token);
        errEl.textContent='';
        document.getElementById('adminLogin').classList.remove('abierto');
        entrarDash();
      } else {
        const data=await res.json().catch(()=>null);
        errEl.textContent=(data&&data.error)?data.error:'Usuario o contraseña incorrectos';
      }
    }catch(e){
      errEl.textContent='No se pudo conectar con el servidor. Verifique que backoffice-service esté corriendo (puerto 8083).';
    }
  }
  function entrarDash(){
    document.getElementById('adminDash').classList.add('abierto');
    cambiarTab('reservas');
  }
  function salirAdmin(){
    const t=tokenAdmin();
    if(t){ fetch(API_BACKOFFICE+'/api/admin/logout',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token:t})}).catch(()=>{}); }
    sessionStorage.removeItem('tampur_admin_token');
    cerrarAdmin();
  }
  function getReservas(){ return JSON.parse(localStorage.getItem('tampur_reservas')||'[]'); }
  function setReservas(r){ localStorage.setItem('tampur_reservas',JSON.stringify(r)); }

  function cambiarTab(tab){
    document.querySelectorAll('.admin-shell aside button[data-tab]').forEach(b=>b.classList.toggle('activo',b.dataset.tab===tab));
    if(tab==='reservas') renderReservas();
    else if(tab==='habitaciones') renderHabitaciones();
    else if(tab==='limpieza') renderLimpieza();
    else if(tab==='caja') renderCaja();
    else if(tab==='tarifas') renderTarifas();
  }

  async function renderReservas(){
    document.getElementById('dashTitulo').textContent='Monitoreo de reservas';
    const cont=document.getElementById('dashContenido');
    try{
      const res=await fetch(API_RESERVAS+'/api/reservas');
      if(!res.ok) throw new Error('offline');
      const rs=await res.json();
      if(rs.length===0){ cont.innerHTML='<p style="color:#888">No hay reservas aún. Las reservas que se hagan desde la web aparecerán aquí automáticamente.</p>'; return; }
      const filas=rs.map(r=>`
        <tr>
          <td><b>${r.codigo}</b></td>
          <td>${r.nombre}<br><small style="color:#999">DNI: ${r.dni}</small></td>
          <td>${r.tipoHabitacion}</td>
          <td>${r.fechaEntrada} → ${r.fechaSalida}</td>
          <td>${r.noches}</td>
          <td>S/ ${r.total}</td>
          <td><span class="estado ${r.estado}">${r.estado}</span></td>
          <td style="white-space:nowrap">
            ${r.estado!=='Confirmada'?`<button class="btn-sm btn-confirmar" onclick="accionReservaBackend('${r.codigo}','Confirmada')">Confirmar</button> `:''}
            ${r.estado!=='Cancelada'?`<button class="btn-sm btn-cancelar" onclick="accionReservaBackend('${r.codigo}','Cancelada')">Cancelar</button> `:''}
            <button class="btn-sm btn-eliminar" onclick="eliminarReservaBackend('${r.codigo}')">Eliminar</button>
          </td>
        </tr>`).join('');
      cont.innerHTML=`<div class="tabla-wrap"><table class="tabla"><thead><tr><th>Código</th><th>Huésped</th><th>Habitación</th><th>Fechas</th><th>Noches</th><th>Total</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>${filas}</tbody></table></div>`;
    }catch(e){
      renderReservasLocal();
    }
  }
  async function accionReservaBackend(codigo, estado){
    try{ await fetch(API_RESERVAS+'/api/reservas/'+codigo+'/estado',{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({estado:estado})}); }catch(e){}
    renderReservas();
  }
  async function eliminarReservaBackend(codigo){
    try{ await fetch(API_RESERVAS+'/api/reservas/'+codigo,{method:'DELETE'}); }catch(e){}
    renderReservas();
  }
  function renderReservasLocal(){
    const cont=document.getElementById('dashContenido');
    const rs=getReservas();
    if(rs.length===0){ cont.innerHTML='<p style="color:#888">No hay reservas aún (sin conexión al servidor).</p>'; return; }
    const filas=rs.map((r,i)=>`
      <tr>
        <td><b>${r.codigo}</b><br><small style="color:#999">${r.creado}</small></td>
        <td>${r.nombre}<br><small style="color:#999">DNI: ${r.dni}</small></td>
        <td>${r.habitacion}</td>
        <td>${r.llegada} → ${r.salida}</td>
        <td>${r.noches}</td>
        <td>S/ ${r.total}</td>
        <td><span class="estado ${r.estado}">${r.estado}</span></td>
        <td style="white-space:nowrap">
          ${r.estado!=='Confirmada'?`<button class="btn-sm btn-confirmar" onclick="accionReservaLocal(${i},'Confirmada')">Confirmar</button> `:''}
          ${r.estado!=='Cancelada'?`<button class="btn-sm btn-cancelar" onclick="accionReservaLocal(${i},'Cancelada')">Cancelar</button> `:''}
          <button class="btn-sm btn-eliminar" onclick="eliminarReservaLocal(${i})">Eliminar</button>
        </td>
      </tr>`).join('');
    cont.innerHTML=`<div class="tabla-wrap"><table class="tabla"><thead><tr><th>Código</th><th>Huésped</th><th>Habitación</th><th>Fechas</th><th>Noches</th><th>Total</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>${filas}</tbody></table></div>`;
  }
  function accionReservaLocal(i,estado){ const rs=getReservas(); rs[i].estado=estado; setReservas(rs); renderReservas(); }
  function eliminarReservaLocal(i){ const rs=getReservas(); rs.splice(i,1); setReservas(rs); renderReservas(); }

  function getHabitaciones(){
    return JSON.parse(localStorage.getItem('tampur_habitaciones')||'[{"id":"S1","tipo":"Simple","estado":"Libre"},{"id":"S2","tipo":"Simple","estado":"Limpieza"},{"id":"D1","tipo":"Doble","estado":"Libre"},{"id":"D2","tipo":"Doble","estado":"Ocupada"},{"id":"M1","tipo":"Matrimonial","estado":"Libre"},{"id":"M2","tipo":"Matrimonial","estado":"Limpieza"}]');
  }
  function setHabitaciones(h){ localStorage.setItem('tampur_habitaciones',JSON.stringify(h)); }
  async function renderHabitaciones(){
    document.getElementById('dashTitulo').textContent='Plano de habitaciones';
    const cont=document.getElementById('dashContenido');
    try{
      const res=await fetch(API_BACKOFFICE+'/api/ocupacion',{headers:headersAdmin()});
      if(!res.ok) throw new Error('offline');
      const habs=await res.json();
      pintarPlano(habs, true);
    }catch(e){
      pintarPlano(getHabitaciones(), false);
    }
  }
  function pintarPlano(habs, desdeServidor){
    const cont=document.getElementById('dashContenido');
    const ordenTipo=['Simple','Doble','Matrimonial'];
    const porTipo={};
    habs.forEach(h=>{ (porTipo[h.tipo]=porTipo[h.tipo]||[]).push(h); });
    const contar=e=>habs.filter(h=>h.estado===e).length;
    const bloques=ordenTipo.map(t=>{
      const filas=(porTipo[t]||[]).map(h=>{
        const accion=desdeServidor
          ? `cambiarEstadoHabBackend('${h.id}','${h.estado}')`
          : `cambiarEstadoHabLocal('${h.id}','${h.estado}')`;
        return `<div class="room room-${h.estado}"><div class="room-top"><span class="room-id">${h.id}</span><span class="room-tipo">${h.tipo}</span></div><button class="estado-plano ${h.estado}" onclick="${accion}">${h.estado}</button></div>`;
      }).join('');
      return filas?`<div class="plano-grupo"><h3>${t}</h3><div class="plano">${filas}</div></div>`:'';
    }).join('');
    cont.innerHTML=`
      <div class="plano-leyenda">
        <span class="chip chip-Libre">Libre</span>
        <span class="chip chip-Ocupada">Ocupada</span>
        <span class="chip chip-Limpieza">Limpieza</span>
        <span class="chip chip-Mantenimiento">Mantenimiento</span>
      </div>
      <div class="tarjetas-kpi">
        <div class="kpi"><div class="num">${habs.length}</div><div class="lbl">Habitaciones</div></div>
        <div class="kpi"><div class="num">${contar('Libre')}</div><div class="lbl">Libres</div></div>
        <div class="kpi"><div class="num">${contar('Ocupada')+contar('Limpieza')+contar('Mantenimiento')}</div><div class="lbl">No disponibles</div></div>
      </div>
      ${bloques}
      <p style="color:#888;margin-top:16px;font-size:.85rem">${desdeServidor?'Haga clic sobre un estado para cambiarlo (desde el servidor).':'Modo local: backend no conectado.'}</p>`;
  }
  async function cambiarEstadoHabBackend(id, estado){
    const orden=['Libre','Ocupada','Limpieza','Mantenimiento'];
    const sig=orden[(orden.indexOf(estado)+1)%orden.length];
    try{ await fetch(API_BACKOFFICE+'/api/ocupacion/'+id+'/estado',{method:'PATCH',headers:headersAdmin(),body:JSON.stringify({estado:sig})}); }catch(e){}
    renderHabitaciones();
  }
  function cambiarEstadoHabLocal(id, estado){
    const h=getHabitaciones(); const orden=['Libre','Ocupada','Limpieza','Mantenimiento'];
    const sig=orden[(orden.indexOf(estado)+1)%orden.length];
    const idx=h.findIndex(x=>x.id===id);
    if(idx>=0){ h[idx].estado=sig; setHabitaciones(h); }
    renderHabitaciones();
  }

  async function renderCaja(){
    document.getElementById('dashTitulo').textContent='Control de caja e ingresos';
    const cont=document.getElementById('dashContenido');
    let rs;
    try{
      const res=await fetch(API_RESERVAS+'/api/reservas');
      if(!res.ok) throw new Error('offline');
      rs=await res.json();
    }catch(e){
      rs=getReservas().map(r=>({total:r.total,estado:r.estado}));
    }
    const confirmadas=rs.filter(r=>r.estado==='Confirmada');
    const pendientes=rs.filter(r=>r.estado==='Pendiente');
    const canceladas=rs.filter(r=>r.estado==='Cancelada');
    const ingConf=confirmadas.reduce((s,r)=>s+r.total,0);
    const ingPend=pendientes.reduce((s,r)=>s+r.total,0);
    cont.innerHTML=`
      <div class="tarjetas-kpi">
        <div class="kpi"><div class="num">S/ ${ingConf+ingPend}</div><div class="lbl">Ingresos (confirmadas + pendientes)</div></div>
        <div class="kpi"><div class="num">${rs.length}</div><div class="lbl">Reservas totales</div></div>
        <div class="kpi"><div class="num">S/ ${ingConf}</div><div class="lbl">Ingresos confirmados</div></div>
      </div>
      <div class="tarjetas-kpi">
        <div class="kpi"><div class="num">${pendientes.length}</div><div class="lbl">Pendientes de confirmar</div></div>
        <div class="kpi"><div class="num">${confirmadas.length}</div><div class="lbl">Confirmadas</div></div>
        <div class="kpi"><div class="num">${canceladas.length}</div><div class="lbl">Canceladas</div></div>
      </div>`;
  }

  function renderTarifas(){
    document.getElementById('dashTitulo').textContent='Gestión de tarifas';
    const t=JSON.parse(localStorage.getItem('tampur_tarifas')||JSON.stringify(TARIFAS));
    document.getElementById('dashContenido').innerHTML=`
      <div class="formulario" style="max-width:440px">
        <div class="campo-form"><label>Habitación Simple (S/ por noche)</label><input type="number" id="tSimple" value="${t.simple}" min="0"></div>
        <div class="campo-form"><label>Habitación Doble (S/ por noche)</label><input type="number" id="tDoble" value="${t.doble}" min="0"></div>
        <div class="campo-form"><label>Habitación Matrimonial (S/ por noche)</label><input type="number" id="tMatrimonial" value="${t.matrimonial}" min="0"></div>
        <button class="btn btn-primario" onclick="guardarTarifas()">Guardar tarifas</button>
      </div>`;
  }
  function guardarTarifas(){
    const t={simple:+document.getElementById('tSimple').value, doble:+document.getElementById('tDoble').value, matrimonial:+document.getElementById('tMatrimonial').value};
    localStorage.setItem('tampur_tarifas',JSON.stringify(t));
    Object.assign(TARIFAS,t);
    cargarTarifas();
    alert('Tarifas actualizadas correctamente');
  }

  cargarTarifas();
  cargarDisponibilidad();
  cargarTasas();
  iniciarMoneda();
  animarContadores();

  // ===================== LIMPIEZA DEL DÍA =====================
  // Estructura del hotel simulada (debe coincidir con backoffice-service): Piso 1 = 101-107, Piso 2 = 201-212, Piso 3 = 301-305.
  const PISOS_LIMPIEZA = [
    {piso:1, desde:101, hasta:107},
    {piso:2, desde:201, hasta:212},
    {piso:3, desde:301, hasta:305}
  ];

  function generarHabitacionesHotel(){
    const habitaciones=[];
    PISOS_LIMPIEZA.forEach(({piso,desde,hasta})=>{
      for(let n=desde;n<=hasta;n++) habitaciones.push({numero:n,piso});
    });
    return habitaciones;
  }

  /** PRNG simple y determinístico (mulberry32), sembrado con la fecha de hoy: mismo resultado todo el día, distinto cada día. Solo se usa como respaldo si el backend no responde. */
  function pseudoAleatorioPorFecha(semillaTexto){
    let h=0;
    for(let i=0;i<semillaTexto.length;i++){ h=Math.imul(31,h)+semillaTexto.charCodeAt(i)|0; }
    return function(){
      h|=0; h=h+0x6D2B79F5|0;
      let t=Math.imul(h^h>>>15,1|h);
      t=t+Math.imul(t^t>>>7,61|t)^t;
      return ((t^t>>>14)>>>0)/4294967296;
    };
  }

  function habitacionesQueNecesitanLimpiezaHoyLocal(){
    const hoy=new Date().toISOString().slice(0,10);
    const azar=pseudoAleatorioPorFecha('tampur-limpieza-'+hoy);
    return generarHabitacionesHotel().filter(()=>azar()<0.35);
  }
  function getLimpiezaMarcadasLocal(){
    const hoy=new Date().toISOString().slice(0,10);
    return new Set(JSON.parse(localStorage.getItem('tampur_limpieza_'+hoy)||'[]'));
  }
  function setLimpiezaMarcadasLocal(marcadas){
    const hoy=new Date().toISOString().slice(0,10);
    localStorage.setItem('tampur_limpieza_'+hoy, JSON.stringify([...marcadas]));
  }

  async function renderLimpieza(){
    document.getElementById('dashTitulo').textContent='Limpieza del día';
    try{
      const res=await fetch(API_BACKOFFICE+'/api/limpieza',{headers:headersAdmin()});
      if(!res.ok) throw new Error('offline');
      const habitaciones=await res.json(); // [{numero,piso,limpiada}]
      const confirmadas=await obtenerReservasConfirmadas();
      pintarLimpieza(habitaciones, true, confirmadas);
    }catch(e){
      const locales=habitacionesQueNecesitanLimpiezaHoyLocal();
      const marcadas=getLimpiezaMarcadasLocal();
      const habitaciones=locales.map(h=>({...h, limpiada:marcadas.has(h.numero)}));
      const confirmadas=getReservas().filter(r=>r.estado==='Confirmada')
        .map(r=>({tipoHabitacion:r.habitacion,nombre:r.nombre,fechaEntrada:r.llegada,fechaSalida:r.salida}));
      pintarLimpieza(habitaciones, false, confirmadas);
    }
  }

  /** Obtiene las reservas confirmadas desde el backend para mostrar sus fechas de ocupación. */
  async function obtenerReservasConfirmadas(){
    try{
      const res=await fetch(API_RESERVAS+'/api/reservas');
      if(!res.ok) return [];
      const rs=await res.json();
      return rs.filter(r=>r.estado==='Confirmada')
        .map(r=>({tipoHabitacion:r.tipoHabitacion,nombre:r.nombre,fechaEntrada:r.fechaEntrada,fechaSalida:r.fechaSalida}));
    }catch(e){ return []; }
  }

  /** Formatea una fecha ISO (yyyy-mm-dd) a un texto legible en español, sin desfase de zona horaria. */
  function formatearFechaOcupacion(iso){
    if(!iso) return '';
    const d=new Date(iso+'T00:00:00');
    return d.toLocaleDateString('es-PE',{weekday:'long',day:'numeric',month:'long'});
  }

  function pintarLimpieza(habitaciones, desdeServidor, confirmadas){
    const cont=document.getElementById('dashContenido');
    const hoy=new Date().toLocaleDateString('es-PE',{weekday:'long',year:'numeric',month:'long',day:'numeric'});

    const confirmadasOrdenadas=[...(confirmadas||[])].sort((a,b)=>(a.fechaEntrada||'').localeCompare(b.fechaEntrada||''));
    const ocupacionesHtml=confirmadasOrdenadas.length?`
      <div class="limp-ocupaciones no-imprimir">
        <h3>🛎️ Ocupaciones próximas · reservas confirmadas</h3>
        <div class="ocupaciones-grid">
          ${confirmadasOrdenadas.map(r=>`
            <div class="ocupacion">
              <div class="ocup-top"><span class="ocup-tipo">Habitación ${r.tipoHabitacion}</span></div>
              <div class="ocup-huesped">${r.nombre}</div>
              <div class="ocup-fecha">
                <span class="ico-fecha"><svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M8 2v4M16 2v4M3 9h18"/></svg></span>
                <span>Check-in: <b>${formatearFechaOcupacion(r.fechaEntrada)}</b> <span class="hora">1:00 p.m.</span></span>
              </div>
              <div class="ocup-fecha">
                <span class="ico-fecha"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/></svg></span>
                <span>Check-out: <b>${formatearFechaOcupacion(r.fechaSalida)}</b> <span class="hora">12:00 p.m.</span></span>
              </div>
            </div>`).join('')}
        </div>
      </div>`:'';

    if(habitaciones.length===0){
      cont.innerHTML=`
        ${ocupacionesHtml}
        <p style="color:#888">No hay habitaciones pendientes de limpieza para hoy.</p>`;
      return;
    }

    const totalLimpiadas=habitaciones.filter(h=>h.limpiada).length;
    const porPiso={};
    habitaciones.forEach(h=>{ (porPiso[h.piso]=porPiso[h.piso]||[]).push(h); });

    const bloquesPiso=Object.keys(porPiso).sort().map(piso=>{
      const filas=porPiso[piso].sort((a,b)=>a.numero-b.numero).map(h=>`
        <label class="limp-item ${h.limpiada?'limpiada':''}">
          <input type="checkbox" ${h.limpiada?'checked':''} onchange="toggleLimpieza(${h.numero}, this.checked, ${desdeServidor})">
          <span class="limp-num">${h.numero}</span>
          <span class="limp-estado">${h.limpiada?'Limpiada':'Pendiente'}</span>
        </label>`).join('');
      return `<div class="limp-piso"><h3>Piso ${piso}</h3><div class="limp-grid">${filas}</div></div>`;
    }).join('');

    cont.innerHTML=`
      <div class="limp-cabecera no-imprimir">
        <div class="tarjetas-kpi" style="margin-bottom:18px">
          <div class="kpi"><div class="num">${habitaciones.length}</div><div class="lbl">Habitaciones para limpiar hoy</div></div>
          <div class="kpi"><div class="num">${totalLimpiadas}</div><div class="lbl">Ya limpiadas</div></div>
          <div class="kpi"><div class="num">${habitaciones.length-totalLimpiadas}</div><div class="lbl">Pendientes</div></div>
        </div>
        <div class="plano-leyenda" style="margin-bottom:14px">
          <span class="chip chip-Limpieza">Pendiente</span>
          <span class="chip chip-Libre">Limpiada</span>
        </div>
        <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
          <button class="btn btn-primario" onclick="imprimirListaLimpieza()">🖨️ Imprimir lista para la encargada</button>
          ${desdeServidor?'':'<span style="color:#c0392b;font-size:.85rem">⚠ Backend no conectado: guardando en modo local.</span>'}
        </div>
      </div>
      ${ocupacionesHtml}
      <div id="listaLimpiezaImprimible">
        <div class="limp-solo-impresion">
          <h2>Hotel Támpur · Lista de limpieza</h2>
          <p>${hoy}</p>
        </div>
        ${bloquesPiso}
      </div>`;
  }

  async function toggleLimpieza(numero, limpiada, desdeServidor){
    if(desdeServidor){
      try{
        await fetch(API_BACKOFFICE+'/api/limpieza/'+numero+'/estado',{
          method:'PATCH', headers:headersAdmin(),
          body:JSON.stringify({limpiada})
        });
      }catch(e){ /* si falla, igual refrescamos: pasará a modo local */ }
    } else {
      const marcadas=getLimpiezaMarcadasLocal();
      limpiada ? marcadas.add(numero) : marcadas.delete(numero);
      setLimpiezaMarcadasLocal(marcadas);
    }
    renderLimpieza();
  }

  function imprimirListaLimpieza(){
    window.print();
  }
