import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { api, errorMessage, fetchAll, fetchPage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Modal from './Modal';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Row = any;

export interface Option { value: string; label: string }

export interface FieldDef {
  name: string;
  label: string;
  type: 'text' | 'number' | 'email' | 'date' | 'password' | 'textarea' | 'checkbox' | 'select' | 'multiselect';
  required?: boolean;
  /** Valeurs statiques (enum) */
  options?: Option[];
  /** Valeurs chargées depuis l'API ; la valeur envoyée est {id} */
  optionsFrom?: { path: string; label: (o: Row) => string };
  /** Champ non modifiable en édition */
  createOnly?: boolean;
  step?: string;
}

export interface Column { header: string; render: (row: Row) => ReactNode; sort?: string }

export interface FilterDef {
  param: string;
  label: string;
  options?: Option[];
  optionsFrom?: { path: string; label: (o: Row) => string };
}

interface Props {
  title: string;
  path: string;
  /** Préfixe de permission, ex. PRODUCT → PRODUCT_WRITE */
  perm: string;
  columns: Column[];
  fields: FieldDef[];
  filters?: FilterDef[];
  canCreate?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  defaults?: Record<string, unknown>;
  extraActions?: (row: Row, reload: () => void) => ReactNode;
  toolbarExtra?: ReactNode;
  reloadKey?: number;
}

export default function CrudPage({
  title, path, perm, columns, fields, filters = [], canCreate = true, canEdit = true, canDelete = true,
  defaults = {}, extraActions, toolbarExtra, reloadKey = 0,
}: Props) {
  const { can } = useAuth();
  const canWrite = can(`${perm}_WRITE`);

  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(0);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [sort, setSort] = useState('id,desc');
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});
  const [options, setOptions] = useState<Record<string, Row[]>>({});
  const [editing, setEditing] = useState<{ id?: number; values: Record<string, unknown> } | null>(null);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');

  const load = useCallback(async () => {
    try {
      const data = await fetchPage<Row>(path, { page, size: 10, sort, q, ...filterValues });
      setRows(data.content);
      setTotal(data.page.totalElements);
      setPages(data.page.totalPages);
      setError('');
    } catch (e) {
      setError(errorMessage(e));
    }
  }, [path, page, sort, q, filterValues]);

  useEffect(() => { void load(); }, [load, reloadKey]);

  // Chargement des listes déroulantes (formulaire + filtres)
  useEffect(() => {
    const paths = new Set<string>();
    fields.forEach((f) => f.optionsFrom && paths.add(f.optionsFrom.path));
    filters.forEach((f) => f.optionsFrom && paths.add(f.optionsFrom.path));
    paths.forEach((p) => {
      fetchAll<Row>(p).then((list) => setOptions((o) => ({ ...o, [p]: list }))).catch(() => undefined);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const optionsOf = (def: { options?: Option[]; optionsFrom?: { path: string; label: (o: Row) => string } }): Option[] =>
    def.options ?? (def.optionsFrom ? (options[def.optionsFrom.path] ?? []).map((o) => ({ value: String(o.id), label: def.optionsFrom!.label(o) })) : []);

  const openForm = (row?: Row) => {
    const values: Record<string, unknown> = { ...defaults };
    if (row) {
      for (const f of fields) {
        const v = row[f.name];
        if (f.type === 'select' && f.optionsFrom) values[f.name] = v?.id != null ? String(v.id) : '';
        else if (f.type === 'multiselect') values[f.name] = (v ?? []).map((x: Row) => String(x.id));
        else if (f.type === 'password') values[f.name] = '';
        else values[f.name] = v ?? (f.type === 'checkbox' ? false : '');
      }
    } else {
      for (const f of fields) {
        if (values[f.name] === undefined) values[f.name] = f.type === 'checkbox' ? false : f.type === 'multiselect' ? [] : '';
      }
    }
    setFormError('');
    setEditing({ id: row?.id, values });
  };

  const toPayload = (values: Record<string, unknown>) => {
    const out: Record<string, unknown> = {};
    for (const f of fields) {
      if (editing?.id && f.createOnly) continue;
      const v = values[f.name];
      if (f.type === 'select' && f.optionsFrom) out[f.name] = v ? { id: Number(v) } : null;
      else if (f.type === 'multiselect') out[f.name] = (v as string[]).map((id) => ({ id: Number(id) }));
      else if (f.type === 'number') out[f.name] = v === '' || v == null ? null : Number(v);
      else if (f.type === 'checkbox') out[f.name] = !!v;
      else out[f.name] = v === '' ? null : v;
    }
    return out;
  };

  const save = async () => {
    if (!editing) return;
    try {
      const payload = toPayload(editing.values);
      if (editing.id) await api.put(`${path}/${editing.id}`, payload);
      else await api.post(path, payload);
      setEditing(null);
      void load();
    } catch (e) {
      setFormError(errorMessage(e));
    }
  };

  const remove = async (row: Row) => {
    if (!window.confirm('Supprimer cet élément ?')) return;
    try {
      await api.delete(`${path}/${row.id}`);
      void load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const setValue = (name: string, v: unknown) => setEditing((s) => (s ? { ...s, values: { ...s.values, [name]: v } } : s));

  const toggleSort = (field: string) => {
    const [f, d] = sort.split(',');
    setSort(f === field && d === 'asc' ? `${field},desc` : `${field},asc`);
    setPage(0);
  };

  return (
    <div>
      <h2>{title}</h2>
      <div className="toolbar">
        <input className="grow" placeholder="Rechercher…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        {filters.map((f) => (
          <select key={f.param} value={filterValues[f.param] ?? ''}
                  onChange={(e) => { setFilterValues((s) => ({ ...s, [f.param]: e.target.value })); setPage(0); }}>
            <option value="">{f.label} : tous</option>
            {optionsOf(f).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        ))}
        {toolbarExtra}
        {canWrite && canCreate && <button onClick={() => openForm()}>+ Nouveau</button>}
      </div>

      {error && <div className="error">{error}</div>}

      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.header} className={c.sort ? 'sortable' : ''} onClick={() => c.sort && toggleSort(c.sort)}>
                {c.header}{c.sort && sort.startsWith(c.sort + ',') ? (sort.endsWith('asc') ? ' ▲' : ' ▼') : ''}
              </th>
            ))}
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              {columns.map((c) => <td key={c.header}>{c.render(r)}</td>)}
              <td className="actions">
                {extraActions?.(r, load)}
                {canWrite && canEdit && <button className="small secondary" onClick={() => openForm(r)}>Modifier</button>}
                {canWrite && canDelete && <button className="small danger" onClick={() => remove(r)}>Suppr.</button>}
              </td>
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={columns.length + 1} className="muted">Aucun résultat</td></tr>}
        </tbody>
      </table>

      <div className="pager">
        <span className="muted">{total} résultat(s)</span>
        <button className="secondary small" disabled={page === 0} onClick={() => setPage(page - 1)}>Précédent</button>
        <span>Page {pages === 0 ? 0 : page + 1} / {pages}</span>
        <button className="secondary small" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Suivant</button>
      </div>

      {editing && (
        <Modal title={editing.id ? `Modifier #${editing.id}` : 'Nouveau'} onClose={() => setEditing(null)}>
          {formError && <div className="error">{formError}</div>}
          <form onSubmit={(e) => { e.preventDefault(); void save(); }}>
            {fields.map((f) => {
              const disabled = !!editing.id && f.createOnly;
              const v = editing.values[f.name];
              return (
                <div className="field" key={f.name}>
                  <label>{f.label}{f.required ? ' *' : ''}</label>
                  {f.type === 'textarea' ? (
                    <textarea rows={3} value={(v as string) ?? ''} disabled={disabled} onChange={(e) => setValue(f.name, e.target.value)} />
                  ) : f.type === 'checkbox' ? (
                    <input type="checkbox" style={{ width: 20 }} checked={!!v} onChange={(e) => setValue(f.name, e.target.checked)} />
                  ) : f.type === 'select' ? (
                    <select value={(v as string) ?? ''} required={f.required} disabled={disabled} onChange={(e) => setValue(f.name, e.target.value)}>
                      <option value="">—</option>
                      {optionsOf(f).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  ) : f.type === 'multiselect' ? (
                    <select multiple size={5} value={(v as string[]) ?? []} disabled={disabled}
                            onChange={(e) => setValue(f.name, Array.from(e.target.selectedOptions).map((o) => o.value))}>
                      {optionsOf(f).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  ) : (
                    <input type={f.type} step={f.step} value={(v as string) ?? ''} required={f.required && !(f.type === 'password' && !!editing.id)}
                           disabled={disabled} placeholder={f.type === 'password' && editing.id ? 'Laisser vide pour ne pas changer' : ''}
                           onChange={(e) => setValue(f.name, e.target.value)} />
                  )}
                </div>
              );
            })}
            <div className="row" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="secondary" onClick={() => setEditing(null)}>Annuler</button>
              <button type="submit">Enregistrer</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
