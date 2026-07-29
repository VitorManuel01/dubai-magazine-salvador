import { useEffect, useState } from 'react';
import { useDadosProdutosMutate } from '../../hooks/useDadosProdutosMutate';
import { DadosProdutos } from '../../interface/DadosProdutos';
import { useAuth } from '../../context/AuthProvider';
import 'bootstrap/dist/css/bootstrap.min.css';
import './cadastrarCliente.css';

interface InputProps {
    label: string;
    value: string | number;
    updateValue(value: any): void;
    type?: string;
}

interface ModalProps {
    closeModal(): void;
}

const Input = ({ label, value, updateValue, type = 'text' }: InputProps) => (
    <div className="mb-3">
        <label className="form-label">{label}</label>
        <input
            className="form-control"
            type={type}
            value={value}
            onChange={(event) => {
                const newValue = type === 'number' ? event.target.valueAsNumber : event.target.value;
                updateValue(newValue);
            }}
        />
    </div>
);

export function CadastrarProdutos({ closeModal }: ModalProps) {
    const [codigoSantri, setCodigoSantri] = useState('');
    const [descricao, setDescricao] = useState('');
    const [ncm, setNcm] = useState('');
    const [unidade, setUnidade] = useState('UN');
    const [marca, setMarca] = useState('');
    const [codigoOriginal, setCodigoOriginal] = useState('');
    const [quantidade, setQuantidade] = useState(0);
    const [precoVenda, setPrecoVenda] = useState(0);
    const [precoVendaIva, setPrecoVendaIva] = useState(0);
    const [categoriaCodigo, setCategoriaCodigo] = useState('');
    const [imagemUrl, setImagemUrl] = useState('');

    const { mutate, isSuccess, isPending } = useDadosProdutosMutate();
    const { funcao } = useAuth();

    useEffect(() => {
        if (isSuccess) closeModal();
    }, [closeModal, isSuccess]);

    if (funcao !== 'ROLE_ADMIN') {
        return <div>Você não tem permissão para acessar esta página.</div>;
    }

    const submit = () => {
        const dadosProdutos: DadosProdutos = {
            codigoSantri,
            descricao,
            nomeExibidoSite: descricao,
            ncm,
            unidade,
            marca,
            codigoOriginal,
            quantidade,
            precoVenda,
            precoVendaIva,
            categoriaCodigo,
            categoriaNome: '',
            categoriaCaminho: '',
            imagemUrl,
            exibirNoSite: true,
            destaqueNaHome: false,
        };

        mutate(dadosProdutos);
    };

    return (
        <div className="modal fade show d-block" tabIndex={-1} role="dialog">
            <div className="modal-dialog modal-lg" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">Cadastrar produto</h5>
                        <button type="button" className="btn-close" aria-label="Fechar" onClick={closeModal} />
                    </div>
                    <div className="modal-body">
                        <form>
                            <Input label="Código Santri" value={codigoSantri} updateValue={setCodigoSantri} />
                            <Input label="Descrição" value={descricao} updateValue={setDescricao} />
                            <Input label="NCM" value={ncm} updateValue={setNcm} />
                            <Input label="Unidade" value={unidade} updateValue={setUnidade} />
                            <Input label="Marca" value={marca} updateValue={setMarca} />
                            <Input label="Código original" value={codigoOriginal} updateValue={setCodigoOriginal} />
                            <Input label="Quantidade" value={quantidade} updateValue={setQuantidade} type="number" />
                            <Input label="Preço de venda" value={precoVenda} updateValue={setPrecoVenda} type="number" />
                            <Input label="Preço de venda IVA" value={precoVendaIva} updateValue={setPrecoVendaIva} type="number" />
                            <Input label="Código da categoria" value={categoriaCodigo} updateValue={setCategoriaCodigo} />
                            <Input label="URL da imagem" value={imagemUrl} updateValue={setImagemUrl} />
                        </form>
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-primary" onClick={submit} disabled={isPending}>
                            {isPending ? 'Cadastrando...' : 'Cadastrar'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
