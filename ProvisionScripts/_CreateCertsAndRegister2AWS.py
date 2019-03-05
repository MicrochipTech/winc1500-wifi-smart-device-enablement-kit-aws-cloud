
import ca_create_root
import ca_create_signer_csr
import ca_create_signer
import aws_register_signer


print('\nExecuting ca_create_root')
ca_create_root.main()
print('\nDone Executing ca_create_root')

print('\nExecuting ca_create_signer_csr')
ca_create_signer_csr.main()
print('\nDone Executing ca_create_signer_csr')

print('\nExecuting ca_create_signer')
ca_create_signer.main()
print('\nDone Executing ca_create_signer')

print('\nExecuting aws_register_signer')
aws_register_signer.main()
print('\nDone Executing aws_register_signer')
